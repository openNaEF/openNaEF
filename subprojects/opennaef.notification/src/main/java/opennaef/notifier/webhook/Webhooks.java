package opennaef.notifier.webhook;

import net.arnx.jsonic.JSON;
import opennaef.notifier.NaefSubscriber;
import opennaef.notifier.NotifyListener;
import opennaef.notifier.filter.FilterQuery;
import opennaef.notifier.util.Logs;
import opennaef.notifier.util.NotFound;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

import javax.persistence.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Webhooks
 * <p>
 * 登録された URL に対して DtoChanges を POST する
 */
public class Webhooks {
    private static final Webhooks _instance = new Webhooks();
    private final EntityManager _em;
    private final TypedQuery<Hook> HOOK_ALL;

    private Webhooks() {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("db");
        _em = factory.createEntityManager();
        HOOK_ALL = _em.createNamedQuery("hook.all", Hook.class);
        Logs.hook.info("db connected.");

        try {
            NaefSubscriber.instance().addListener(new Listener());
            Logs.common.info("CommitNotifier installed.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static Webhooks instance() {
        return Webhooks._instance;
    }

    public List<Hook> hooks() {
        return HOOK_ALL.getResultList();
    }

    public Hook hook(long id) throws NotFound {
        Optional<Hook> hook = Optional.ofNullable(_em.find(Hook.class, id));
        if (hook.isPresent()) {
            return hook.get();
        }
        throw new NotFound();
    }

    public Hook add(Hook raw) throws PingFailed, NotFound {
        // hook 登録前に ping チェック
        boolean success = Webhooks.instance().sendPing(raw);
        if (!success) {
            // ping に失敗した場合は bad request
            throw new PingFailed(raw.getId(), raw.getCallbackURL());
        }

        _em.getTransaction().begin();
        _em.persist(raw);
        _em.getTransaction().commit();
        Logs.hook.info("add: {} {} {}", raw.getId(), raw.getCallbackURL().toString(), raw.isActive());
        return hook(raw.getId());
    }

    public Hook update(long id, Map patch) throws NotFound, PingFailed {
        _em.getTransaction().begin();

        Hook hook = hook(id);
        String prevCallbackURL = hook.getCallbackURL().toString();

        Optional.ofNullable(patch.get(Hook.CALLBACK_URL)).ifPresent(v -> {
            try {
                hook.setCallbackURL(new URL(Objects.toString(v, null)));
            } catch (MalformedURLException e) {
                // ignore
            }
        });
        Optional.ofNullable(patch.get(Hook.ACTIVE)).ifPresent(v -> hook.setActive(Boolean.parseBoolean(Objects.toString(v))));

        Optional.ofNullable(patch.get(Hook.FILTER)).ifPresent(v -> {
            FilterQuery filter = JSON.decode(JSON.encode(v), FilterQuery.class);
            hook.setFilter(filter);
        });

        // callback_url が変わったら ping チェック
        // active == true なら ping チェック
        if (!prevCallbackURL.equals(hook.getCallbackURL().toString()) || hook.isActive()) {
            boolean success = Webhooks.instance().sendPing(hook);
            if (!success) {
                // ping に失敗した場合は bad request
                PingFailed failed = new PingFailed(hook.getId(), hook.getCallbackURL());
                _em.getTransaction().rollback();
                throw failed;
            }
        }

        _em.merge(hook);
        _em.getTransaction().commit();
        Logs.hook.info("update: {} {} {}", hook.getId(), hook.getCallbackURL().toString(), hook.isActive());
        return hook;
    }

    public boolean remove(long id) throws NotFound {
        EntityTransaction tx = _em.getTransaction();
        tx.begin();
        Hook target = hook(id);
        _em.remove(target);
        tx.commit();
        Logs.hook.info("remove: {} {} {}", target.getId(), target.getCallbackURL().toString(), target.isActive());
        return true;
    }

    public void send(DtoChanges dtoChanges) {
        Logs.hook.info("send {}", dtoChanges.getTargetVersion().toString());
        try {
            HookAggregater.sendDtoChanges(dtoChanges, hooks());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * callback_url に対して ping リクエストを送信する
     *
     * @param hook hook
     * @return ping に成功した場合に true
     */
    public boolean sendPing(Hook hook) {
        Logs.hook.info("ping {}", hook.getCallbackURL());
        return HookAggregater.sendPing(hook);

    }

    /**
     * POST通知に失敗した場合は以後通知を行わない
     *
     * @param hook hook
     */
    public void notifyFailed(Hook hook) {
        Logs.hook.debug("NOTIFY FAILED. {}: {}", hook.getId(), hook.getCallbackURL());
        hook.setFailed(true);
        hook.setMessage("NOTIFY FAILED");
    }

    /**
     * POST成功
     *
     * @param hook hook
     */
    public void notifySuccess(Hook hook) {
        Logs.hook.debug("notify success. {}: {}", hook.getId(), hook.getCallbackURL());
        hook.setFailed(false);
        hook.setMessage(null);
    }

    static class Listener implements NotifyListener {
        @Override
        public void transactionCommitted(TransactionId txId, DtoChanges dtoChanges) {
            Webhooks.instance().send(dtoChanges);
        }
    }

    public static void main(String[] args) throws Exception {
        Webhooks service = Webhooks.instance();
        Hook h1 = new Hook(new URL("http://new"));
        service.add(h1);
        Hook h2 = new Hook(new URL("http://update"));
        service.add(h2);
        Map<String, Object> updatePatch = new HashMap<>();
        updatePatch.put("callback_url", "http://update/update/update");
        service.update(h2.getId(), updatePatch);

        updatePatch.put("active", false);
        service.update(h2.getId(), updatePatch);

        for (Hook hook : service.hooks()) {
            System.out.println(hook.getId() + " " + hook.getCallbackURL() + " " + hook.isActive());
        }

        System.out.println("--");
        service.remove(h1.getId());
        for (Hook hook : service.hooks()) {
            System.out.println(hook.getId() + " " + hook.getCallbackURL() + " " + hook.isActive());
        }
    }
}