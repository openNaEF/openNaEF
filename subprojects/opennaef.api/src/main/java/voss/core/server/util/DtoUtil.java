package voss.core.server.util;

import naef.dto.*;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.DateTime;
import tef.MVO.MvoId;
import tef.TransactionId;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.RmiDtoFacade;
import tef.skelton.dto.DtoOriginator;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.EntityDto.Desc;
import tef.skelton.dto.MvoDtoDesc;
import tef.skelton.dto.MvoDtoOriginator;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;

import java.text.SimpleDateFormat;
import java.util.*;

public class DtoUtil {

    public static EntityDto getDto(MvoId id) {
        if (id == null) {
            return null;
        }
        try {
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            return facade.getMvoDto(id, null);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static EntityDto getDto(MvoId id, int version) {
        if (id == null) {
            return null;
        }
        try {
            TransactionId.W w = new TransactionId.W(version);
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            return facade.getMvoDto(id, w);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static MvoId getMvoId(EntityDto dto) {
        if (dto == null) {
            return null;
        }
        Desc<?> desc = dto.getDescriptor();
        if (desc == null) {
            throw new IllegalStateException("no descriptor: " + dto);
        }
        return getMvoId(desc);
    }

    public static MvoId getMvoId(Desc<?> desc) {
        if (desc == null) {
            return null;
        } else if (!MvoDtoDesc.class.isInstance(desc)) {
            throw new IllegalStateException("not supported descriptor type: " + desc.getClass().getName());
        }
        MvoDtoDesc<?> mvoDescriptor = MvoDtoDesc.class.cast(desc);
        return mvoDescriptor.getMvoId();
    }

    public static String getMvoIdString(EntityDto dto) {
        MvoId id = getMvoId(dto);
        if (id == null) {
            return null;
        }
        return id.toString();
    }

    public static MvoDtoOriginator getMvoDtoOriginator(EntityDto dto) {
        if (dto == null) {
            return null;
        }
        DtoOriginator originator = dto.originator();
        if (originator == null) {
            throw new IllegalStateException("no originator: " + dto);
        } else if (!MvoDtoOriginator.class.isInstance(originator)) {
            throw new IllegalStateException("not supported originator type: " + originator.getClass().getName());
        }
        return MvoDtoOriginator.class.cast(originator);
    }

    public static RmiDtoFacade getRmiDtoFacade(EntityDto dto) {
        MvoDtoOriginator originator = getMvoDtoOriginator(dto);
        if (originator == null) {
            return null;
        }
        return originator.getRmiDtoFacade();
    }

    public static NaefDtoFacade getNaefDtoFacade(EntityDto dto) {
        RmiDtoFacade facade = getRmiDtoFacade(dto);
        if (facade == null) {
            return null;
        } else if (NaefDtoFacade.class.isInstance(facade)) {
            return NaefDtoFacade.class.cast(facade);
        } else {
            throw new IllegalStateException("unexpected type: " + facade.getClass().getName());
        }
    }

    public static TransactionId.W getMvoVersion(EntityDto dto) {
        if (dto == null) {
            return null;
        }
        Desc<?> desc = dto.getDescriptor();
        if (!MvoDtoDesc.class.isInstance(desc)) {
            return null;
        }
        MvoDtoDesc<?> mvoDescriptor = MvoDtoDesc.class.cast(desc);
        return mvoDescriptor.getVersion();
    }

    public static String getMvoVersionString(EntityDto dto) {
        TransactionId.W id = getMvoVersion(dto);
        if (id == null) {
            return null;
        }
        return id.toString();
    }

    public static TransactionId.W getMvoTimestamp(EntityDto dto) {
        if (dto == null) {
            return null;
        }
        Desc<?> desc = dto.getDescriptor();
        if (!MvoDtoDesc.class.isInstance(desc)) {
            return null;
        }
        MvoDtoDesc<?> mvoDescriptor = MvoDtoDesc.class.cast(desc);
        return mvoDescriptor.getTimestamp();
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityDto> T getPreviousVersionDto(T dto) {
        if (dto == null) {
            return null;
        }
        try {
            TransactionId.W current = getMvoVersion(dto);
            TransactionId.W w = new TransactionId.W(current.serial - 1);
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            return (T) facade.getMvoDto(getMvoId(dto), w);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static List<Integer> getMvoVersions(EntityDto dto) {
        List<Integer> history = new ArrayList<Integer>();
        if (dto == null) {
            return history;
        }
        history.addAll(getMvoVersions(getMvoId(dto)));
        return history;
    }

    public static List<Integer> getMvoVersions(MvoId id) {
        List<Integer> history = new ArrayList<Integer>();
        if (id == null) {
            return history;
        }
        try {
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            List<TransactionId.W> versions = facade.getVersions(id);
            for (TransactionId.W version : versions) {
                history.add(Integer.valueOf(version.serial));
            }
            Collections.sort(history);
            return history;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String getAbsoluteName(EntityDto dto) {
        if (dto == null) {
            return null;
        } else if (NaefDto.class.isInstance(dto)) {
            return ((NaefDto) dto).getAbsoluteName();
        } else {
            return getMvoId(dto).toString();
        }
    }

    public static String getString(EntityDto dto, String key) {
        String value = getStringOrNull(dto, key);
        if (value == null) {
            return "";
        }
        return value;
    }

    public static boolean getBoolean(EntityDto dto, String key) {
        if (dto == null) {
            return false;
        }
        Object value = dto.getValue(key);
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else {
            throw new IllegalArgumentException("value is not boolean or equivalent: " + value.getClass().getName());
        }
    }

    public static Integer getInteger(EntityDto dto, String key) {
        if (dto == null) {
            return null;
        }
        Object value = dto.getValue(key);
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else {
            throw new IllegalArgumentException("value is not integer: " + value.getClass().getName());
        }
    }

    public static int getIntegerOrZero(EntityDto dto, String key) {
        if (dto == null) {
            return 0;
        }
        Object value = dto.getValue(key);
        if (value == null) {
            return 0;
        } else if (value instanceof Integer) {
            return ((Integer) value).intValue();
        } else {
            throw new IllegalArgumentException("value is not integer: " + value.getClass().getName());
        }
    }

    public static Double getDouble(EntityDto dto, String key) {
        if (dto == null) {
            return null;
        }
        Object value = dto.getValue(key);
        if (value == null) {
            return null;
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            Integer i = (Integer) value;
            return Double.valueOf(i.doubleValue());
        } else if (value instanceof Long) {
            Long l = (Long) value;
            return Double.valueOf(l.doubleValue());
        } else {
            throw new IllegalArgumentException("value is not double: " + value.getClass().getName());
        }
    }

    public static List<IdRange<Integer>> getIdRanges(NaefDto dto, String key) {
        Object o = dto.getValue(key);
        List<IdRange<Integer>> result = new ArrayList<IdRange<Integer>>();
        if (o == null) {
            return result;
        } else if (o instanceof Set<?>) {
            Set<?> set = (Set<?>) o;
            for (Object element : set) {
                if (element instanceof IdRange.Integer) {
                    result.add((IdRange.Integer) element);
                }
            }
            Collections.sort(result, new Comparator<IdRange<Integer>>() {
                @Override
                public int compare(IdRange<Integer> o1, IdRange<Integer> o2) {
                    int diff = o1.lowerBound - o2.lowerBound;
                    if (diff != 0) {
                        return diff;
                    }
                    return o1.upperBound - o2.upperBound;
                }
            });
            return result;
        } else {
            throw new IllegalStateException("unexpected data type: "
                    + o.getClass().getName() + ", " + o.toString());
        }
    }

    public static String getIdRangesString(NaefDto dto, String key) {
        List<IdRange<Integer>> ranges = getIdRanges(dto, key);
        StringBuilder sb = new StringBuilder();
        for (IdRange<Integer> range : ranges) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            if (range.upperBound == range.lowerBound) {
                sb.append(range.upperBound);
            } else {
                sb.append(range.lowerBound).append("-").append(range.upperBound);
            }
        }
        return sb.toString();
    }

    public static List<String> getIdRangesAsUniformFormat(PortDto port, String key) {
        List<String> result = new ArrayList<String>();
        Object o = port.getValue(key);
        if (o == null) {
            return result;
        } else if (o instanceof Set<?>) {
            Set<?> set = (Set<?>) o;
            for (Object element : set) {
                if (element instanceof IdRange<?>) {
                    result.add(((IdRange<?>) element).lowerBound + "-" + ((IdRange<?>) element).upperBound);
                }
            }
            Collections.sort(result);
            return result;
        } else {
            throw new IllegalStateException("unexpected data type: "
                    + o.getClass().getName() + ", " + o.toString());
        }
    }

    public static Long getLong(EntityDto dto, String key) {
        if (dto == null) {
            return null;
        }
        Object value = dto.getValue(key);
        if (value == null) {
            return null;
        } else if (value instanceof Long) {
            return (Long) value;
        } else {
            throw new IllegalArgumentException("value is not long: " + value.getClass().getName());
        }
    }

    public static String getString(EntityDto dto, String key, String defaultValue) {
        String value = getStringOrNull(dto, key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static String getStringOrNull(EntityDto dto, String key) {
        if (dto == null) {
            return null;
        }
        Object value = dto.getValue(key);
        return getStringOrNull(value);
    }

    public static String getStringOrNull(Object value) {
        Logger log = LoggerFactory.getLogger(DtoUtil.class);
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            return b.toString();
        } else if (value instanceof String) {
            String s = (String) value;
            return Util.stringToNull(s);
        } else if (value instanceof Integer) {
            Integer i = (Integer) value;
            return i.toString();
        } else if (value instanceof Long) {
            Long l = (Long) value;
            return l.toString();
        } else if (value instanceof DateTime) {
            Date d = ((DateTime) value).toJavaDate();
            return DtoUtil.getMvoDateFormat().format(d);
        } else if (value instanceof Set<?>) {
            Set<?> set = (Set<?>) value;
            List<Object> values = new ArrayList<Object>();
            for (Object o : set) {
                if (o instanceof String) {
                    values.add(o);
                } else if (o instanceof IdRange<?>) {
                    IdRange<?> range = (IdRange<?>) o;
                    Object upper = range.upperBound;
                    Object lower = range.lowerBound;
                    if (upper.equals(lower)) {
                        values.add(lower);
                    } else {
                        String s = lower + "-" + upper;
                        values.add(s);
                    }
                } else {
                    values.add(o);
                }
            }
            Collections.sort(values, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    } else if (o2 == null) {
                        return -1;
                    } else if (o1 == null) {
                        return 1;
                    }
                    Long _l1 = toLong(o1);
                    Long _l2 = toLong(o2);
                    if (_l1 == null && _l2 == null) {
                        return o1.toString().compareTo(o2.toString());
                    } else if (_l1 == null) {
                        return 1;
                    } else if (_l2 == null) {
                        return -1;
                    }
                    if (_l1.longValue() < _l2.longValue()) {
                        return -1;
                    } else if (_l1.longValue() > _l2.longValue()) {
                        return 1;
                    }
                    return 0;
                }

                private Long toLong(Object o) {
                    if (o instanceof Long) {
                        return ((Long) o).longValue();
                    } else if (o instanceof Integer) {
                        return (long) ((Integer) o).intValue();
                    }
                    String s = o.toString();
                    if (s.indexOf('-') != -1) {
                        String[] arr = s.split("-");
                        return toLong(arr[0]);
                    } else {
                        return toLong(s);
                    }
                }

                private Long toLong(String s) {
                    try {
                        long val = Long.parseLong(s);
                        return val;
                    } catch (NumberFormatException e) {
                    }
                    return null;
                }
            });
            StringBuilder sb = new StringBuilder();
            for (Object v : values) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(v);
            }
            return sb.toString();
        } else if (value instanceof Desc<?>) {
            Desc<?> ref = (Desc<?>) value;
            try {
                EntityDto attrDto = CoreConnector.getInstance().getDtoFacade().getMvoDto(ref);
                if (attrDto instanceof NaefDto) {
                    NaefDto naefDto = (NaefDto) attrDto;
                    return naefDto.getAbsoluteName();
                } else {
                    log.warn("- ignored: unexpected attribute type: " +
                            "mvo-id=[" + getMvoId(attrDto) + "]");
                    return getMvoId(attrDto).toString();
                }
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }

        } else {
            String s = value.toString();
            return Util.stringToNull(s);
        }
    }

    public static boolean isValueObjectChanged(EntityDto dto, String attrName, Object value) {
        if (dto == null) {
            return value != null;
        }
        Object o = dto.getValue(attrName);
        if (o == null && value == null) {
            return false;
        } else if (o != null && value == null) {
            return true;
        } else if (o == null && value != null) {
            return true;
        }
        return !o.equals(value);
    }

    public static boolean isValueChanged(EntityDto dto, String attrName, String value) {
        return !hasStringValue(dto, attrName, value);
    }

    public static boolean isValueChanged(EntityDto dto, String attrName, Long value) {
        return !hasLongValue(dto, attrName, value);
    }

    public static boolean isValueChanged(EntityDto dto, String attrName, Integer value) {
        return !hasIntegerValue(dto, attrName, value);
    }

    public static boolean isValueChanged(EntityDto dto, String attrName, Boolean value) {
        return !hasBooleanValue(dto, attrName, value);
    }

    public static boolean hasStringValue(EntityDto dto, String attrName, String value) {
        if (dto == null) {
            return value == null;
        }
        String val = getStringOrNull(dto, attrName);
        if (val == null && value == null) {
            return true;
        } else if (val == null) {
            return false;
        } else if (value == null) {
            return false;
        } else {
            return val.equals(value);
        }
    }

    public static boolean hasLongValue(EntityDto dto, String attrName, Long value) {
        if (dto == null) {
            return value == null;
        }
        Long val = getLong(dto, attrName);
        if (val == null && value == null) {
            return true;
        } else if (val == null) {
            return false;
        } else if (value == null) {
            return false;
        } else {
            return val.equals(value);
        }
    }

    public static boolean hasIntegerValue(EntityDto dto, String attrName, Integer value) {
        if (dto == null) {
            return value == null;
        }
        Integer val = getInteger(dto, attrName);
        if (val == null && value == null) {
            return true;
        } else if (val == null) {
            return false;
        } else if (value == null) {
            return false;
        } else {
            return val.equals(value);
        }
    }

    public static boolean hasBooleanValue(EntityDto dto, String attrName, Boolean value) {
        if (dto == null) {
            return false;
        } else if (value == null) {
            return false;
        }
        boolean val = getBoolean(dto, attrName);
        return val == value.booleanValue();
    }

    public static boolean hasSameAttributeValue(EntityDto dto1, EntityDto dto2, String attrName) {
        if (attrName == null) {
            throw new IllegalArgumentException();
        }
        if (dto1 == null || dto2 == null) {
            return false;
        }
        Object o1 = dto1.getValue(attrName);
        Object o2 = dto2.getValue(attrName);
        return Util.equals(o1, o2);
    }

    public static Map<String, String> getValues(EntityDto dto) {
        Map<String, String> result = new HashMap<String, String>();
        if (dto == null) {
            return result;
        }
        for (String key : dto.getAttributeNames()) {
            result.put(key, DtoUtil.getString(dto, key));
        }
        return result;
    }

    public static String getDateTime(EntityDto dto, String key) {
        if (dto == null) {
            return null;
        }
        Object o = dto.getValue(key);
        if (o == null) {
            return null;
        }
        if (o instanceof DateTime) {
            DateTime datetime = (DateTime) o;
            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            return df.format(datetime.toJavaDate());
        }
        return o.toString();
    }

    public static Date getDate(EntityDto dto, String key) {
        if (dto == null) {
            return null;
        }
        Object o = dto.getValue(key);
        if (o == null) {
            return null;
        }
        if (!(o instanceof DateTime)) {
            throw new IllegalStateException("[" + key + "] is not datetime attribute. " + o.getClass().getName());
        }
        DateTime datetime = (DateTime) o;
        return datetime.toJavaDate();
    }

    public static String getIfName(PortDto port) {
        return getStringOrNull(port, getIfNameAttributeName());
    }

    public static String getConfigName(PortDto port) {
        String configName = getStringOrNull(port, ATTR.CONFIG_NAME);
        if (configName != null) {
            return configName;
        }
        return getIfName(port);
    }

    public static void putValues(Map<String, String> prop, String key, EntityDto dto) {
        if (dto == null) {
            return;
        }
        Object o = dto.getValue(key);
        if (o == null) {
            return;
        } else if (o instanceof String) {
            prop.put(key, (String) o);
        } else if (o instanceof DateTime) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
            Date d = ((DateTime) o).toJavaDate();
            prop.put(key, df.format(d));
        } else {
            prop.put(key, o.toString());
        }
    }

    public static void putValuesWithDefault(Map<String, String> prop, String key,
                                            EntityDto dto, String defaultValue) {
        if (dto == null) {
            return;
        }
        Object o = dto.getValue(key);
        if (o == null) {
            prop.put(key, defaultValue);
        } else if (o instanceof String) {
            prop.put(key, (String) o);
        } else if (o instanceof DateTime) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
            Date d = ((DateTime) o).toJavaDate();
            prop.put(key, df.format(d));
        } else {
            prop.put(key, o.toString());
        }
    }

    public static List<String> getStringList(NaefDto dto, String attr) {
        List<String> result = new ArrayList<String>();
        if (dto == null || attr == null) {
            return result;
        }
        Object o = dto.getValue(attr);
        if (o == null) {
            return result;
        } else if (o instanceof Set<?>) {
            Set<?> set = (Set<?>) o;
            for (Object element : set) {
                String s = mvoValueToString(dto, element);
                result.add(s);
            }
        } else if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            for (Object element : list) {
                String s = mvoValueToString(dto, element);
                result.add(s);
            }
        } else {
            String msg = getUnexpectedAttributeErrorMessage("unexpected data type:", dto, attr, o);
            throw new IllegalStateException(msg);
        }
        return result;
    }

    public static Map<String, String> getStringMap(NaefDto dto, String attr) {
        Map<String, String> result = new HashMap<String, String>();
        if (dto == null) {
            return result;
        }
        Object o = dto.getValue(attr);
        if (o == null) {
            return result;
        } else if (o instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String k = mvoValueToString(dto, entry.getKey());
                String v = mvoValueToString(dto, entry.getValue());
                result.put(k, v);
            }
        } else {
            String msg = getUnexpectedAttributeErrorMessage("unexpected data type:", dto, attr, o);
            throw new IllegalStateException(msg);
        }
        return result;
    }

    private static String getUnexpectedAttributeErrorMessage(String head, NaefDto dto, String attr, Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(head).append(" \r\n\t");
        sb.append("dto=").append(dto.getAbsoluteName());
        sb.append("\r\n\t");
        sb.append("class=").append(dto.getClass().getName());
        sb.append(" ");
        sb.append("attr=").append(attr);
        sb.append("\r\n\t");
        sb.append("attrClass=").append(o.getClass().getName());
        sb.append("\r\n\t");
        sb.append("attrValue=").append(o.toString());
        return sb.toString();
    }

    public static String mvoValueToString(EntityDto dto, Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof CustomerInfoDto) {
            return ((CustomerInfoDto) value).getName();
        } else if (value instanceof Desc<?>) {
            Desc<?> ref = (Desc<?>) value;
            NaefDto obj = (NaefDto) dto.toDto(ref);
            return obj.getAbsoluteName();
        } else if (value instanceof IdRange<?>) {
            IdRange<?> range = (IdRange<?>) value;
            return range.lowerBound + "-" + range.upperBound;
        } else {
            throw new IllegalStateException("unknown class:"
                    + value.getClass().getName() + ", " + value.toString());
        }
    }

    public static <T extends NaefDto> T getInstanceOf(Class<T> cls, NaefDto... args) {
        if (cls == null) {
            return null;
        } else if (args.length == 0) {
            return null;
        }
        for (NaefDto arg : args) {
            if (cls.isInstance(arg)) {
                return cls.cast(arg);
            }
        }
        return null;
    }

    public static boolean isSupportedAttribute(NaefDto dto, String attributeName) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            for (String attrName : conn.getSupportedAttributeNames(dto)) {
                if (attrName.equals(attributeName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static Set<String> getSupportedAttributeNames(NaefDto dto) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            return conn.getSupportedAttributeNames(dto);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Attribute<?, ?>> getAttributeMap(EntityDto dto) {
        Map<String, Attribute<?, ?>> result = new HashMap<String, Attribute<?, ?>>();
        try {
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            List<Attribute<?, ?>> attributes = facade.getDeclaredAttributes((Class<EntityDto>) dto.getClass());
            for (Attribute<?, ?> attribute : attributes) {
                String name = attribute.getName();
                result.put(name, attribute);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    public static Class<?> getAttributeClass(EntityDto dto, String attrName) {
        Logger log = LoggerFactory.getLogger(DtoUtil.class);
        try {
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            List<Attribute<?, ?>> attributes = facade.getDeclaredAttributes((Class<EntityDto>) dto.getClass());
            for (Attribute<?, ?> attribute : attributes) {
                String name = attribute.getName();
                AttributeType<?> type = attribute.getType();
                log.debug(name + ":" + type.getJavaType().getName());
                if (name.equals(attrName)) {
                    return type.getJavaType();
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    public static NetworkDto getNetwork(NaefDto element) {
        if (element == null) {
            return null;
        } else if (element instanceof VlanIfDto) {
            return ((VlanIfDto) element).getTrafficDomain();
        } else if (element instanceof VplsIfDto) {
            return ((VplsIfDto) element).getTrafficDomain();
        } else if (element instanceof VrfIfDto) {
            return ((VrfIfDto) element).getTrafficDomain();
        }
        return null;
    }

    public static boolean isSameMvoEntity(NaefDto dto1, NaefDto dto2) {
        return mvoEquals(dto1, dto2);
    }

    public static boolean mvoEquals(NaefDto dto1, NaefDto dto2) {
        if (dto1 == null || dto2 == null) {
            return false;
        }
        return getMvoId(dto1).equals(getMvoId(dto2));
    }

    public static boolean isChanged(NaefDto dto1, NaefDto dto2) {
        if (dto1 == null && dto2 == null) {
            return false;
        } else if (dto1 == null && dto2 != null) {
            return true;
        } else if (dto1 != null && dto2 == null) {
            return true;
        }
        return !isSameMvoEntity(dto1, dto2);
    }

    public static <T extends EntityDto> List<T> removeDuplication(List<T> list) {
        List<T> result = new ArrayList<T>();
        Set<MvoId> occured = new HashSet<MvoId>();
        for (T dto : list) {
            if (dto == null) {
                continue;
            }
            MvoId id = getMvoId(dto);
            if (occured.contains(id)) {
                continue;
            }
            result.add(dto);
            occured.add(id);
        }

        return result;
    }

    public static SimpleDateFormat getMvoDateFormat() {
        return new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
    }

    public static SimpleDateFormat getMvoDateFormatForMilliSecond() {
        return new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss.SSS");
    }

    public static String getStringID(VplsDto v1) {
        if (v1 == null) {
            return null;
        } else if (v1.getStringId() != null) {
            return v1.getStringId();
        } else if (v1.getIntegerId() != null) {
            return v1.getIntegerId().toString();
        }
        return null;
    }

    public static String getStringID(VrfDto v1) {
        if (v1 == null) {
            return null;
        } else if (v1.getStringId() != null) {
            return v1.getStringId();
        } else if (v1.getIntegerId() != null) {
            return v1.getIntegerId().toString();
        }
        return null;
    }

    public static <T extends EntityDto> Set<T> getInstances(Class<T> class_, Collection<? extends EntityDto> set) {
        Set<T> result = new HashSet<T>();
        if (set == null) {
            return result;
        } else if (class_ == null) {
            throw new IllegalArgumentException("class_ is null.");
        }
        for (EntityDto dto : set) {
            if (class_.isInstance(dto)) {
                result.add(class_.cast(dto));
            }
        }
        return result;
    }

    public static Desc<NodeDto> getNodeRef(NodeElementDto dto) {
        if (dto == null) {
            return null;
        }
        Desc<NodeDto> ref = dto.get(NodeElementDto.ExtAttr.NODE);
        return ref;
    }

    public static Desc<NodeElementDto> getOwnerRef(NodeElementDto dto) {
        if (dto == null) {
            return null;
        }
        Desc<NodeElementDto> ref = dto.get(NodeElementDto.ExtAttr.OWNER);
        return ref;
    }

    public static Class<? extends EntityDto> getOwnerClass(NodeElementDto dto) {
        if (dto == null) {
            return null;
        }
        try {
            Desc<NodeElementDto> ref = getOwnerRef(dto);
            if (ref == null) {
                return null;
            }
            RmiDtoFacade facade = getRmiDtoFacade(dto);
            return facade.getDtoClass(getMvoId(ref));
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean isOwnerClass(Class<? extends NodeElementDto> ownerClass, NodeElementDto dto) {
        if (dto == null) {
            return false;
        }
        try {
            Class<? extends EntityDto> c = getOwnerClass(dto);
            if (c == null) {
                return false;
            }
            return ownerClass.isAssignableFrom(c);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String toDebugString(EntityDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append(toMvoString(dto));
        if (NaefDto.class.isInstance(dto)) {
            sb.append("=[");
            sb.append(getAbsoluteName(dto));
            sb.append("]");
        }
        return sb.toString();
    }

    public static String toStringOrMvoString(Object o) {
        return toStringOrMvoString(o, null, true);
    }

    public static String toStringOrMvoString(Object o, NaefDto dto) {
        return toStringOrMvoString(o, dto, true);
    }

    public static String toStringOrMvoString(Object o, boolean resolveCollection) {
        return toStringOrMvoString(o, null, resolveCollection);
    }

    public static String toStringOrMvoString(Object o, NaefDto dto, boolean resolveCollection) {
        if (o == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (Collection.class.isInstance(o)) {
            Collection<?> collection = (Collection<?>) o;
            if (resolveCollection) {
                sb.append(toMvoCollectionString(collection, dto, false));
            } else {
                sb.append(o.getClass().getSimpleName()).append("{").append(collection.size()).append("}");
            }
        } else if (String.class.isInstance(o)) {
            sb.append((String) o);
        } else if (EntityDto.class.isInstance(o)) {
            sb.append(toMvoString((EntityDto) o));
        } else if (Desc.class.isInstance(o)) {
            Desc<?> ref = (Desc<?>) o;
            if (dto == null) {
                sb.append("MvoDto.Ref[").append(getMvoId(ref).toString()).append("]");
            } else {
                try {
                    Class<?> _class = getRmiDtoFacade(dto).getDtoClass(getMvoId(ref));
                    sb.append(toMvoString(_class, getMvoId(ref)));
                } catch (Exception e) {
                    throw new IllegalStateException("Unexpected exception.", e);
                }
            }
        } else {
            sb.append(o.toString());
        }
        return sb.toString();
    }

    public static String toMvoString(EntityDto dto) {
        if (dto == null) {
            return null;
        }
        return toMvoString(dto.getClass(), getMvoId(dto));
    }

    public static String toMvoString(Class<?> cls, MvoId id) {
        return cls.getSimpleName() + "[" + id.toString() + "]";
    }

    public static String toMvoCollectionString(Collection<?> collection, boolean recursive) {
        return toMvoCollectionString(collection, null, recursive);
    }

    public static String toMvoCollectionString(Collection<?> collection, NaefDto dto, boolean recursive) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("{");
        for (Object e : collection) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(toStringOrMvoString(e, dto, recursive));
        }
        sb.append("}");
        return sb.toString();
    }

    public static String getIfNameAttributeName() {
        return CoreConfiguration.getInstance().getAttributePolicy().getIfNameAttributeName();
    }
}