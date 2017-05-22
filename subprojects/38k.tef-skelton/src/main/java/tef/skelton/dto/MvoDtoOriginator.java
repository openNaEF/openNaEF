package tef.skelton.dto;

import tef.MVO;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.RmiDtoFacade;
import tef.skelton.SkeltonTefService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MvoDtoOriginator implements DtoOriginator {

    private final RmiDtoFacade facade_;

    public MvoDtoOriginator(RmiDtoFacade facade) {
        facade_ = facade;
    }

    public RmiDtoFacade getRmiDtoFacade() {
        return facade_;
    }

    @Override public EntityDto getDto(EntityDto.Oid oid) {
        MvoOid mvoid = (MvoOid) oid;
        if (tefService() != null) {
            TransactionContext.setupReadTransaction();

            return buildDtoInternal(mvoid.oid);
        } else {
            try {
                return facade_.getMvoDto(mvoid.oid, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override public <T extends EntityDto> T getDto(EntityDto.Desc<T> desc) {
        if (desc == null) {
            return null;
        }

        MvoDtoDesc<T> ref = (MvoDtoDesc<T>) desc;
        if (tefService() != null) { 
            TransactionContext.setupReadTransaction();

            return buildDto(ref);
        } else {
            try {
                return facade_.getMvoDto(ref);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override public <T extends EntityDto> List<T> getDtosList(List<? extends EntityDto.Desc<T>> descs) {
        if (descs == null) {
            return null;
        }

        if (tefService() != null) {
            TransactionContext.setupReadTransaction();

            return buildDtoCollection(new ArrayList<T>(), (List<MvoDtoDesc<T>>) descs);
        } else {
            try {
                return facade_.getMvoDtosList(descs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override public <T extends EntityDto> Set<T> getDtosSet(Set<? extends EntityDto.Desc<T>> descs) {
        if (descs == null) {
            return null;
        }

        if (tefService() != null) {
            TransactionContext.setupReadTransaction();

            return buildDtoCollection(new HashSet<T>(), (Set<MvoDtoDesc<T>>) descs);
        } else {
            try {
                return facade_.getMvoDtosSet(descs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private <C extends Collection<T>, T extends EntityDto>
        C buildDtoCollection(C result, Collection<MvoDtoDesc<T>> descs)
    {
        for (MvoDtoDesc<T> desc : descs) {
            result.add(desc == null ? null : buildDto(desc));
        }
        return result;
    }

    @Override public Object getAttributeValue(EntityDto.Desc<?> desc, String attrName) {
        MvoDtoDesc<?> ref = (MvoDtoDesc<?>) desc;
        if (tefService() != null) { 
            TransactionContext.setupReadTransaction();

            return tefService().getMvoDtoFactory().getAttributeValue(ref, attrName);
        } else {
            try {
                return facade_.getAttributeValue(ref, attrName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static SkeltonTefService tefService() {
        return SkeltonTefService.instance();
    }

    private <T extends EntityDto> T buildDto(MvoDtoDesc<T> ref) {
        if (tefService() == null) { 
            throw new IllegalStateException();
        }

        if (ref == null) {
            return null;
        }

        TransactionId.W savedVersion = TransactionContext.getTargetVersion();
        long savedTime = TransactionContext.getTargetTime();
        try {
            TransactionContext.setTargetVersion(ref.getTimestamp());
            TransactionContext.setTargetTime(ref.getTime());

            return (T) buildDtoInternal(ref.getMvoId());
        } finally {
            TransactionContext.setTargetVersion(savedVersion);
            TransactionContext.setTargetTime(savedTime);
        }
    }

    private EntityDto buildDtoInternal(MVO.MvoId mvoid) {
        return tefService().getMvoDtoFactory().build(this, tefService().getMvoRegistry().get(mvoid));
    }
}
