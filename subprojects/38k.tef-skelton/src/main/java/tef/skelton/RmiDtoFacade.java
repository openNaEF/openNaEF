package tef.skelton;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import tef.MVO;
import tef.TransactionId;
import tef.skelton.dto.DtoChangeListener;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;

public interface RmiDtoFacade extends Remote {

    public Class<? extends EntityDto> getDtoClass(MVO.MvoId mvoId)
        throws AuthenticationException, RemoteException;
    public List<Attribute<?, ?>> getDeclaredAttributes(Class<? extends EntityDto> klass)
        throws AuthenticationException, RemoteException;
    public Attribute<?, ?> getDeclaredAttribute(Class<? extends EntityDto> klass, String attributeName)
        throws AuthenticationException, RemoteException;
    public List<Attribute<?, ?>> getSerializableAttributes(Class<? extends EntityDto> klass)
        throws AuthenticationException, RemoteException;
    public List<TransactionId.W> getVersions(MVO.MvoId mvoId)
        throws AuthenticationException, RemoteException;
    public EntityDto getMvoDto(MVO.MvoId mvoId, TransactionId.W version)
        throws AuthenticationException, RemoteException;
    public <T extends EntityDto> T getMvoDto(EntityDto.Desc<? extends T> ref)
        throws AuthenticationException, RemoteException;
    public <T extends EntityDto> List<T> getMvoDtosList(List<? extends EntityDto.Desc<?>> refs)
        throws AuthenticationException, RemoteException;
    public <T extends EntityDto> Set<T> getMvoDtosSet(Set<? extends EntityDto.Desc<?>> refs)
        throws AuthenticationException, RemoteException;
    public Object getAttributeValue(EntityDto.Desc<?> ref, String attrName)
        throws AuthenticationException, RemoteException;

    /**
     * 属性値の変更履歴を返します。
     */
    public <T> SortedMap<TransactionId.W, T> getAttributeHistory(MVO.MvoId mvoId, String attributeName)
        throws AuthenticationException, RemoteException;

    /**
     * 引数で与えられた MVO ID の文字列表現をサーバで動作する TEF サービスの MVO ID に変換します。
     **/
    public MVO.MvoId toMvoId(String mvoId)
        throws AuthenticationException, RemoteException;

    /**
     * 引数で与えられた文字列を絶対名として解釈して得られた MVO の MVO ID を返します。
     **/
    public MVO.MvoId resolveMvoId(String name)
        throws ResolveException, AuthenticationException, RemoteException;

    public String getTaskName(EntityDto dto)
        throws AuthenticationException, RemoteException;
    public EntityDto buildTaskSynthesizedDto(EntityDto dto)
        throws AuthenticationException, RemoteException;

    public void addDtoChangeListener(String listenerName, DtoChangeListener listener)
        throws AuthenticationException, RemoteException;

    public DtoChanges getDtoChanges(
        TransactionId.W lowerTxid,
        boolean lowerInclusive,
        TransactionId.W upperTxid,
        boolean upperInclusive)
        throws AuthenticationException, RemoteException;
}
