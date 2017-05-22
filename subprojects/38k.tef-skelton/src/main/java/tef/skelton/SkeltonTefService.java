package tef.skelton;

import lib38k.rmc.MethodCall;
import lib38k.rmc.MethodExec;
import lib38k.rmc.RmcServerService;
import lib38k.rmc.service.FileTransfer;
import lib38k.xml.Xml;
import tef.DateTime;
import tef.ExtraObjectCoder;
import tef.MVO;
import tef.ObjectComparator;
import tef.TefInitializationFailedException;
import tef.TefService;
import tef.skelton.dto.DtoAttrTranscript;
import tef.skelton.dto.DtoInitializer;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoFactory;
import tef.skelton.dto.MvoDtoMapping;
import tef.skelton.dto.TaskSynthesizedDtoBuilder;
import tef.ui.ObjectRenderer;
import tef.ui.http.DumpObjectResponse;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SkeltonTefService extends TefService {

    public static SkeltonTefService instance() {
        return (SkeltonTefService) TefService.instance();
    }

    private static final String RMC_SERVER_PORT_ATTRNAME = "rmc-server-port";
    private static final String RMC_FILETRANSFER_CLIENTLIBSPATH = "path.client-libs";

    private SkeltonRmcServerService rmcService_ = null;

    private MvoDtoFactory mvoDtoFactory_;
    private TaskSynthesizedDtoBuilder taskSynthesizedDtoBuilder_;

    private final Resolver.Instances resolvers_ = new Resolver.Instances();
    private final TypeAttributes.Instances typesAttributes_ = new TypeAttributes.Instances();
    private final UiTypeName.Instances typeNames_ = new UiTypeName.Instances();

    public SkeltonTefService() {
        mvoDtoFactory_ = newMvoDtoFactory();
        taskSynthesizedDtoBuilder_ = newTaskSynthesizedDtoBuilder();

        logMessage("tef-skelton service, build:" + readBuildVersion("tef-skelton"));
    }

    /**
     * TEFサービス初期化時に実行される処理を記述します. サブタイプでこのメソッドをオーバーライド
     * する場合はスーパータイプの <code>super.serviceInitializing()</code> 呼び出しを記述するように
     * してください.
     */
    @Override protected void serviceInitializing() {
        super.serviceInitializing();

        installTypes();

        new AttributeMetaInitializer(this).initialize();
        new AttributeMetaInterlockInitializer().initialize();
        MvoDtoMapping.setupByConfigurationFile(mvoDtoFactory_.getMapping());

        setupObjectQuerySingleValueAccesses();
        setupObjectQueryCollectionValueAccesses();

        ObjectComparator.addExtraComparator(
            UiTypeName.class,
            new Comparator<UiTypeName>() {

                @Override public int compare(UiTypeName o1, UiTypeName o2) {
                    return o1.name().compareTo(o2.name());
                }
            });

        addExtraObjectCoder(new ExtraObjectCoder<UiTypeName>("tef.type", UiTypeName.class) {

            @Override public String encode(UiTypeName object) {
                return object.name();
            }

            @Override public UiTypeName decode(String str) {
                UiTypeName result = SkeltonTefService.instance().uiTypeNames().getByName(str);
                if (result == null) {
                    throw new IllegalArgumentException("no such type: " + str);
                }
                return result;
            }
        });

        getSystemProperties().set(SystemPropertyIds.SQN_DELIMITER, ".");
        getSystemProperties().set(SystemPropertyIds.FQN_PRIMARY_DELIMITER, ";");
        getSystemProperties().set(SystemPropertyIds.FQN_SECONDARY_DELIMITER, ",");
        getSystemProperties().set(SystemPropertyIds.FQN_TERTIARY_DELIMITER, "|");
        getSystemProperties().set(SystemPropertyIds.FQN_LEFT_BRACKET, "{");
        getSystemProperties().set(SystemPropertyIds.FQN_RIGHT_BRACKET, "}");

        ObjectRenderer.addObjectRenderer(new ObjectRenderer.Renderer() {

            @Override public Class<?> getTargetClass() {
                return UiTypeName.class;
            }

            @Override public String render(Object obj) {
                return ((UiTypeName) obj).name();
            }
        });

        DumpObjectResponse.addMethodInvoker(new DumpObjectResponse.MethodInvoker() {

            @Override public boolean isAdaptive(MVO obj) {
                return obj instanceof IdPool<?, ?, ?>;
            }

            @Override public String getName() {
                return "escaped fqn as shell commandline arg";
            }

            @Override public Object invokeMethod(MVO obj) {
                IdPool<?, ?, ?> pool = (IdPool<?, ?, ?>) obj;
                return SkeltonUtils.shellCommandlineArgEscape(pool.getFqn());
            }
        });
    }

    @Override protected void serviceStarted() {
        super.serviceStarted();

        if (getTefServiceConfig().getRoot().hasAttr(RMC_SERVER_PORT_ATTRNAME)) {
            setupRmcServerService();
        }
    }

    private void setupRmcServerService() {
        Xml.Elem rootElem = getTefServiceConfig().getRoot();

        int rmcPort = rootElem.getNonNullIntAttr(RMC_SERVER_PORT_ATTRNAME);
        rmcService_ = new SkeltonRmcServerService(rmcPort, createLogger("rmc"));

        if (rootElem.hasAttr(RMC_FILETRANSFER_CLIENTLIBSPATH)) {
            String clientlibspath = rootElem.getAttr(RMC_FILETRANSFER_CLIENTLIBSPATH);
            if (! clientlibspath.equals("")) {
                FileTransfer.TransferFileExec filetransfer = new FileTransfer.TransferFileExec();
                filetransfer.enableDirectory("client-libs", new File(clientlibspath));
                rmcService_.registerMethod(FileTransfer.TransferFile.class, filetransfer);
                rmcService_.registerMethod(
                    FileTransfer.GetFileMeta.class,
                    new FileTransfer.GetFileMetaExec(filetransfer));
            }
        }

        Xml.Elem rmcServerElem = rootElem.getSubElem("rmc-server");
        if (rmcServerElem != null) {
            for (Xml.Elem rmcServiceElem : rmcServerElem.getSubElems("rmc-service")) {
                String rmcCallClassName = rmcServiceElem.getNonNullAttr("call");
                String rmcExecClassName = rmcServiceElem.getNonNullAttr("exec");

                try {
                    Class<? extends MethodCall<Object>> rmcCallClass
                        = (Class<? extends MethodCall<Object>>) Class.forName(rmcCallClassName);
                    Class<? extends MethodExec<MethodCall<Object>, ?>> rmcExecClass
                        = (Class<? extends MethodExec<MethodCall<Object>, ?>>) Class.forName(rmcExecClassName);

                    rmcService_.registerMethod(rmcCallClass, rmcExecClass.newInstance());
                } catch (Exception e) {
                    String errorMessage
                        = "rmc service registration failed, call:" + rmcCallClassName
                        + ", exec:" + rmcExecClassName;
                    logError(errorMessage, e);
                    throw new TefInitializationFailedException(e);
                }
            }
        }

        rmcService_.start();
    }

    protected void installTypes() {
        installUiTypeName(String.class, "string");
        installUiTypeName(Boolean.class, "boolean");
        installUiTypeName(Integer.class, "integer");
        installUiTypeName(Long.class, "long");
        installUiTypeName(Float.class, "float");
        installUiTypeName(Double.class, "double");
        installUiTypeName(DateTime.class, "date-time");
        installUiTypeName(UiTypeName.class, "type");

        installAttributes(TefConnector.class, TefConnector.Attr.class);

        installSubResolver(new Resolver.SingleNameResolver<Model, Model>(Model.class, "mvo-id", null) {

            @Override protected Model resolveImpl(Model context, String arg)
                throws ResolveException
            {
                String idStr = arg;

                MVO.MvoId mvoid;
                try {
                    mvoid = MVO.MvoId.getInstanceByLocalId(idStr);
                } catch (Exception e) {
                    throw new ResolveException("ID の形式を確認してください.");
                }

                MVO obj = TefService.instance().getMvoRegistry().get(mvoid);
                if (obj == null) {
                    throw new ResolveException("オブジェクトが見つかりません: " + idStr);
                }

                if (! (obj instanceof Model)) {
                    throw new ResolveException("コンテキストに設定できないオブジェクトです.");
                }

                return (Model) obj;
            }

            @Override public String getName(Model obj) {
                Resolver resolver = getResolver(obj.getClass());
                return resolver == null
                    ? null
                    : resolver.getName(obj);
            }
        });
        installSubResolver(new Resolver.SingleNameResolver<Model, Model>(Model.class, "attribute", Model.class) {

            @Override protected Model resolveImpl(Model context, String arg)
                throws ResolveException
            {
                String attrName = arg;

                Attribute.SingleAttr<?, ?> attr = getSingleAttribute(context.getClass(), attrName);
                Object value = ((Attribute.SingleAttr<Object, Model>) attr).get(context);
                if (value == null) {
                    throw new ResolveException(attrName + " に値が設定されていません.");
                }
                if (! (value instanceof Model)) {
                    throw new ResolveException(attrName + " の値はコンテキスト設定可能なモデルではありません.");
                }
                return (Model) value;
            }

            private Attribute.SingleAttr<?, ?> getSingleAttribute(Class<? extends Model> klass, String attrName)
                throws ResolveException
            {
                Attribute<?, ?> attr = Attribute.getAttribute(klass, attrName);
                if (attr == null) {
                    throw new ResolveException("属性定義がありません: " + attrName);
                }
                if (! (attr instanceof Attribute.SingleAttr<?, ?>)) {
                    throw new ResolveException("指定された属性は単数値属性ではありません.");
                }
                return (Attribute.SingleAttr<?, ?>) attr;
            }

            @Override public String getName(Model obj) {
                return ((MVO) obj).getMvoId().getLocalStringExpression();
            }
        });
        installSubResolver(new Resolver.SingleNameResolver<Model, IdPool.SingleMap>(
            Model.class, "id", IdPool.SingleMap.class)
        {
            @Override protected Model resolveImpl(IdPool.SingleMap context, String arg)
                throws ResolveException
            {
                String idStr = arg;

                try {
                    Model idUser = (Model) context.getUserByIdString(idStr);
                    if (idUser == null) {
                        throw new ResolveException("対象が見つかりません.");
                    }
                    return idUser;
                } catch (FormatException fe) {
                    throw new ResolveException(fe.getMessage());
                } catch (IdPool.PoolException pe) {
                    throw new ResolveException(pe.getMessage());
                }
            }

            @Override public String getName(Model obj) {
                return uiTypeNames().getName(obj.getClass()) + ":" + ((MVO) obj).getMvoId().getLocalStringExpression();
            }
        });
    }

    /**
     * 型のUI表示名の登録を行います.
     */
    protected void installUiTypeName(Class<?> type, String name) {
        new UiTypeName(typeNames_, type, name);
    }

    protected MvoDtoFactory newMvoDtoFactory() {
        return new MvoDtoFactory(this, newMvoDtoMapping());
    }

    public RmcServerService getRmcServerService() {
        return rmcService_;
    }

    public MvoDtoFactory getMvoDtoFactory() {
        return mvoDtoFactory_;
    }

    protected MvoDtoMapping newMvoDtoMapping() {
        return new MvoDtoMapping();
    }

    public MvoDtoMapping getMvoDtoMapping() {
        return getMvoDtoFactory().getMapping();
    }

    public void installMvoDtoMapping(Class<? extends Model> mvoClass, Class<? extends EntityDto> dtoClass) {
        getMvoDtoMapping().map(mvoClass, dtoClass);
    }

    public void installDtoInitializer(DtoInitializer<?, ?> dtoInitializer) {
        getMvoDtoFactory().addInitializer(dtoInitializer);
    }

    protected TaskSynthesizedDtoBuilder newTaskSynthesizedDtoBuilder() {
        return new TaskSynthesizedDtoBuilder();
    }

    public TaskSynthesizedDtoBuilder getTaskSynthesizedDtoBuilder() {
        return taskSynthesizedDtoBuilder_;
    }

    /**
     * 登録されている全てのリゾルバを返します.
     */
    public synchronized Set<Resolver<?>> getResolvers() {
        return resolvers_.getResolvers();
    }

    /**
     * 引数で指定された名前を持つリゾルバを返します.
     */
    public synchronized Resolver<?> getResolver(String name) {
        return resolvers_.getResolver(name);
    }

    /**
     * 引数で指定された型に対する主リゾルバを返します.
     *
     * @see #installMainResolver(Resolver)
     */
    public synchronized Resolver<?> getResolver(Class<?> type) {
        return resolvers_.getResolver(type);
    }

    /**
     * 型に対する主リゾルバの登録を行います. 主リゾルバとして登録したリゾルバは
     * {@link #getResolver(String) 名前で検索} するか, または 
     * {@link #getResolver(Class) 型から検索} することができます.
     *
     * @see #installSubResolver(Resolver)
     */
    public void installMainResolver(Resolver<?> resolver) {
        resolvers_.installMainResolver(resolver);
    }

    /**
     * 型に対する副リゾルバの登録を行います. 副リゾルバとして登録したリゾルバは
     * {@link #getResolver(String) 名前から検索} できますが, 
     * {@link #getResolver(Class) 型からの検索} はできません.
     *
     * @see #installMainResolver(Resolver)
     */
    public void installSubResolver(Resolver<?> resolver) {
        resolvers_.installSubResolver(resolver);
    }

    public void removeResolver(Resolver<?> resolver) {
        resolvers_.removeResolver(resolver);
    }

    public String getSqnDelimiter() {
        return getSystemProperties().get(SystemPropertyIds.SQN_DELIMITER);
    }

    public String getFqnPrimaryDelimiter() {
        return getSystemProperties().get(SystemPropertyIds.FQN_PRIMARY_DELIMITER);
    }

    public String getFqnSecondaryDelimiter() {
        return getSystemProperties().get(SystemPropertyIds.FQN_SECONDARY_DELIMITER);
    }

    public String getFqnTertiaryDelimiter() {
        return getSystemProperties().get(SystemPropertyIds.FQN_TERTIARY_DELIMITER);
    }

    public String getFqnLeftBracket() {
        return getSystemProperties().get(SystemPropertyIds.FQN_LEFT_BRACKET);
    }

    public String getFqnRightBracket() {
        return getSystemProperties().get(SystemPropertyIds.FQN_RIGHT_BRACKET);
    }

    protected void setupObjectQuerySingleValueAccesses() {
        ObjectQueryExpression.installSingleValueAccess(
            "name",
            new ObjectQueryExpression.SingleValueAccess() {

                @Override public Object get(Object o) {
                    return NamedModel.class.cast(o).getName();
                }
            });
        ObjectQueryExpression.installSingleValueAccess(
            "id-pool.parent",
            new ObjectQueryExpression.SingleValueAccess() {

                @Override public Object get(Object o) {
                    return IdPool.SingleMap.class.cast(o).getParent();
                }
            });
    }

    protected void setupObjectQueryCollectionValueAccesses() {
        ObjectQueryExpression.installCollectionValueAccess(
            "id-pool.users",
            new ObjectQueryExpression.CollectionValueAccess() {

                @Override public Collection<?> get(Object o) {
                    return IdPool.SingleMap.class.cast(o).getUsers();
                }
            });
        ObjectQueryExpression.installCollectionValueAccess(
            "id-pool.children",
            new ObjectQueryExpression.CollectionValueAccess() {

                @Override public Collection<?> get(Object o) {
                    return IdPool.SingleMap.class.cast(o).getChildren();
                }
            });
    }

    public TypeAttributes.Instances getTypesAttributes() {
        return typesAttributes_;
    }

    public synchronized void installAttributes(Class<? extends Model> type, Class<?>... attributeDefinedClasses) {
        for (Class<?> attributeClass : attributeDefinedClasses) {
            for (Attribute<?, ?> attr : Attribute.getDeclaredAttributes(attributeClass)) {
                installAttributes(type, (Attribute<?, Model>) attr);
            }
        }
    }

    public synchronized <T extends Model> void installAttributes(Class<T> type, Attribute<?, ? super T>... attributes) {
        for (Attribute<?, ? super T> attribute : attributes) {
            typesAttributes_.gainInstance(type).install(attribute);
        }
    }

    /**
     * 引数に与えられた map から値が null のエントリ (null mapping) を取り除くユーティリティ 
     * メソッドです.
     * <p>
     * これは, TEF rev.932 より前の MVO.M1 は null をマップすることで remove を表現していた, 
     * すなわち, null mapping と remove の区別がなく, null mapping を removed とみなすものとして
     * いたため, DTO 転写などにおいて値を取り出した後に null mapping を取り除くという操作が必要と
     * されていたためです. このメソッドはそれをサポートするものです.
     * <p>
     * 最新の TEF では null mapping とエントリの削除は区別されるようになっています. このため, 
     * TEF rev.932 より前の null mapping は removed として正しく扱われるようにデータ変換を行うのが
     * 望ましく, それが全てにおいて行われたことが確認された後にこのメソッドは廃止される予定です.
     */
    @Deprecated public static <K, V> Map<K, V> removeNullMapping(Map<K, V> map) {
        for (K key : new HashSet<K>(map.keySet())) {
            if (map.get(key) == null) {
                map.remove(key);
            }
        }
        return map;
    }

    public UiTypeName.Instances uiTypeNames() {
        return typeNames_;
    }

    public void installDtoAttrTranscript(DtoAttrTranscript<?, ?> transcript) {
        getMvoDtoFactory().addDtoAttrTranscript(transcript);
    }
}
