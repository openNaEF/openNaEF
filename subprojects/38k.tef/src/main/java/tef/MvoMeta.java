package tef;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

final class MvoMeta {

    private static class IdMapping<T> {

        private int size_ = 0;
        private final List<T> list_ = new ArrayList<T>();

        void put(int id, T obj) {
            while (list_.size() <= id) {
                list_.add(null);
            }
            if (list_.get(id) != null) {
                throw new IllegalStateException();
            }

            list_.set(id, obj);

            size_++;
        }

        T get(int id) {
            return id < list_.size() ? list_.get(id) : null;
        }

        int size() {
            return size_;
        }
    }

    private static final Class[] DEFAULT_CONSTRUCTOR_ARG_TYPE = new Class[]{MVO.MvoId.class};

    private Set<Class<? extends MVO>> wellFormedMvoClasses_ = new HashSet<Class<? extends MVO>>();

    private int nextClassId_ = 0;
    private int nextFieldId_ = 0;
    private IdMapping<Class<? extends MVO>> idToClasses_ = new IdMapping<Class<? extends MVO>>();
    private Map<Class<? extends MVO>, Integer> classToIds_
            = new HashMap<Class<? extends MVO>, Integer>();

    private Map<Class<? extends MVO>, Constructor<? extends MVO>> constructors_
            = new HashMap<Class<? extends MVO>, Constructor<? extends MVO>>();

    private IdMapping<Field> idToFields_ = new IdMapping<Field>();
    private Map<Field, Integer> fieldToIds_ = new HashMap<Field, Integer>();

    synchronized void validateClassFormat(Class<? extends MVO> clazz) {
        if (wellFormedMvoClasses_.contains(clazz)) {
            return;
        }

        try {
            getMvoConstructorImpl(clazz);
        } catch (NoSuchMethodException nsme) {
            throw new MvoClassFormatException.ConstructorDefinition
                    ("no mvo constructor found: " + clazz.getName());
        }

        for (Field field : getReflectionFields(clazz)) {
            String fieldFqn = clazz.getName() + "." + field.getName();
            if (MVO.MvoField.class.isAssignableFrom(field.getType())) {
                if ((field.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
                    throw new MvoClassFormatException.FieldDefinition
                            ("mvo-field must be final: " + fieldFqn);
                }
                if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                    throw new MvoClassFormatException.FieldDefinition
                            ("mvo-field must not be static: " + fieldFqn);
                }
                if ((field.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT) {
                    throw new MvoClassFormatException.FieldDefinition
                            ("mvo-field is transient: " + fieldFqn);
                }
            } else {
                if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                    continue;
                }
                if ((field.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT) {
                    continue;
                }

                throw new MvoClassFormatException.FieldDefinition
                        ("the field is not serializable: " + fieldFqn);
            }
        }

        wellFormedMvoClasses_.add(clazz);
    }

    synchronized void registerClass(Class<? extends MVO> clazz, Integer classId) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        if (idToClasses_.size() != classToIds_.size()) {
            throw new IllegalStateException();
        }
        if (!isNewClass(clazz)) {
            throw new IllegalStateException();
        }

        classId = classId == null ? nextClassId_ : classId;
        nextClassId_ = Math.max(nextClassId_, classId) + 1;
        if (idToClasses_.get(classId) != null) {
            throw new IllegalStateException();
        }

        classToIds_.put(clazz, classId);
        idToClasses_.put(classId, clazz);
    }

    synchronized boolean isNewClass(Class<? extends MVO> clazz) {
        return !classToIds_.containsKey(clazz);
    }

    synchronized int getClassId(Class<? extends MVO> clazz) {
        return classToIds_.get(clazz);
    }

    synchronized Class<? extends MVO> resolveClass(int id) {
        Class<? extends MVO> result = idToClasses_.get(id);
        if (result == null) {
            throw new JournalRestorationException("no such class: " + id);
        }
        return result;
    }

    private Constructor getMvoConstructorImpl(Class<? extends MVO> clazz)
            throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(DEFAULT_CONSTRUCTOR_ARG_TYPE);
    }

    synchronized Constructor<? extends MVO> getMvoConstructor(Class<? extends MVO> clazz) {
        Constructor constructor = constructors_.get(clazz);
        if (constructor == null) {
            try {
                constructor = getMvoConstructorImpl(clazz);
            } catch (NoSuchMethodException nsme) {
                throw new JournalRestorationException
                        ("no mvo constructor found: " + clazz.getName());
            }

            try {
                constructor.setAccessible(true);
            } catch (Exception e) {
                throw new JournalRestorationException(e);
            }

            constructors_.put(clazz, constructor);
        }

        return constructor;
    }

    synchronized void registerField(Field mvoField, Integer fieldId) {
        if (mvoField == null) {
            throw new IllegalArgumentException();
        }
        if (idToFields_.size() != fieldToIds_.size()) {
            throw new IllegalStateException();
        }
        if (!MVO.MvoField.class.isAssignableFrom(mvoField.getType())) {
            throw new IllegalArgumentException();
        }
        if (!isNewField(mvoField)) {
            throw new IllegalStateException();
        }

        try {
            mvoField.setAccessible(true);
        } catch (Exception e) {
            throw new JournalRestorationException(e);
        }

        fieldId = fieldId == null ? nextFieldId_ : fieldId;
        nextFieldId_ = Math.max(nextFieldId_, fieldId) + 1;
        if (idToFields_.get(fieldId) != null) {
            throw new IllegalStateException();
        }

        fieldToIds_.put(mvoField, fieldId);
        idToFields_.put(fieldId, mvoField);
    }

    synchronized int getFieldId(MVO.MvoField mvoField) {
        return getFieldId(getReflectionField(mvoField));
    }

    synchronized int getFieldId(Field field) {
        Integer result = fieldToIds_.get(field);
        if (result == null) {
            throw new RuntimeException
                    ("no field->id mapping: "
                            + field.getDeclaringClass().getName() + "." + field.getName());
        }
        return result.intValue();
    }

    synchronized boolean isNewField(MVO.MvoField mvoField) {
        return isNewField(getReflectionField(mvoField));
    }

    synchronized boolean isNewField(Field field) {
        return !fieldToIds_.containsKey(field);
    }

    synchronized MVO.MvoField resolveMvoFieldObject(final MVO mvo, final int fieldId)
            throws JournalRestorationException {
        final Field mvoField = resolveReflectionField(fieldId);
        if (mvoField == null) {
            throw new JournalRestorationException
                    ("no such field: " + fieldId + "(" + mvo.getClass().getName() + ")");
        }

        try {
            MVO.MvoField result = (MVO.MvoField) mvoField.get(mvo);
            if (result == null) {
                throw new IllegalStateException(Integer.toString(fieldId, 16));
            }

            return result;
        } catch (Exception e) {
            throw new JournalRestorationException(e);
        }
    }

    synchronized Field resolveReflectionField(int id) {
        Field result = idToFields_.get(id);
        if (result == null) {
            throw new JournalRestorationException("no such field: " + id);
        }
        return result;
    }

    private static final Map<Class, Field[]> fieldsCache__ = new HashMap<Class, Field[]>();

    public static synchronized Field[] getReflectionFields(Class<? extends MVO> c) {
        Field[] result = fieldsCache__.get(c);
        if (result != null) {
            return result;
        }

        if (c == MVO.class) {
            return c.getDeclaredFields();
        }

        Field[] superClassFields = getReflectionFields((Class<? extends MVO>) c.getSuperclass());
        Field[] thisClassFields = c.getDeclaredFields();
        result = new Field[superClassFields.length + thisClassFields.length];
        System.arraycopy(superClassFields, 0, result, 0, superClassFields.length);
        System.arraycopy
                (thisClassFields, 0, result, superClassFields.length, thisClassFields.length);
        for (int i = 0; i < result.length; i++) {
            result[i].setAccessible(true);
        }
        fieldsCache__.put(c, result);
        return result;
    }

    private static final Map<Class, Field[]> mvofieldsCache__ = new HashMap<Class, Field[]>();

    static synchronized Field[] getReflectionMvoFields(Class<? extends MVO> c) {
        Field[] result = mvofieldsCache__.get(c);
        if (result != null) {
            return result;
        }

        List<Field> list = new ArrayList<Field>();
        for (Field field : getReflectionFields(c)) {
            if (MVO.MvoField.class.isAssignableFrom(field.getType())) {
                list.add(field);
            }
        }
        result = list.toArray(new Field[0]);
        mvofieldsCache__.put(c, result);
        return result;
    }

    static List<MVO.MvoField> getMvoFieldObjects(MVO mvo) {
        List<MVO.MvoField> result = new ArrayList<MVO.MvoField>();
        for (Field field : getReflectionMvoFields(mvo.getClass())) {
            try {
                result.add((MVO.MvoField) field.get(mvo));
            } catch (IllegalAccessException iae) {
                TefService.instance().logError(mvo.getMvoId() + "," + field.getName(), iae);
                throw new RuntimeException(iae);
            }
        }
        return result;
    }

    static String getFieldName(MVO.MvoField mvofield) {
        return getReflectionField(mvofield).getName();
    }

    static Field getReflectionField(MVO.MvoField mvofield) {
        MVO parent = mvofield.getParent();
        for (Field field : getReflectionMvoFields(parent.getClass())) {
            try {
                if (field.get(parent) == mvofield) {
                    return field;
                }
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException(iae);
            }
        }

        throw new IllegalStateException();
    }

    static <T extends MVO.MvoField> List<T> selectMvoFieldObjects(MVO mvo, Class<T> fieldType) {
        List<T> result = new ArrayList<T>();
        for (MVO.MvoField field : MvoMeta.getMvoFieldObjects(mvo)) {
            if (fieldType.isAssignableFrom(field.getClass())) {
                result.add((T) field);
            }
        }
        return result;
    }
}
