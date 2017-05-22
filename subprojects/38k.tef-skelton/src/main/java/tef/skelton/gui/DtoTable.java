package tef.skelton.gui;

import lib38k.gui.Table;
import tef.skelton.Attribute;
import tef.skelton.Reloadable;
import tef.skelton.dto.Dto;
import tef.skelton.dto.EntityDto;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DtoTable<T extends Dto> extends Table<T> implements Reloadable {

    public static class DtoColumn<T extends Dto> extends Table.Column<T> {

        private Attribute<?, tef.skelton.Model> attr_;

        public <S> DtoColumn(String columnName, Attribute<S, ?> attr, Class<S> klass) {
            super(columnName, klass);

            attr_ = (Attribute<?, tef.skelton.Model>) attr;
        }

        public <S> DtoColumn(Attribute<S, ?> attr, Class<S> klass) {
            this(attr.getName(), attr, klass);
        }

        public DtoColumn(Attribute<String, ?> attr) {
            this(attr, String.class);
        }

        @Override public Object getValue(T row) {
            return row.get(attr_);
        }
    }

    private volatile Integer highlightedRowIndex_;

    public DtoTable() {
        this(null, null);
    }

    public DtoTable(String nameColumnName) {
        this(nameColumnName, null);
    }

    public DtoTable(List<? extends Attribute<?, ?>> attributes) {
        this(null, attributes);
    }

    public DtoTable(String nameColumnName, List<? extends Attribute<?, ?>> attributes) {
        super(new Table.Model<T>());

        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);

        if (nameColumnName != null) {
            setNameColumnName(nameColumnName);
        }

        if (attributes != null) {
            addColumns(attributes);
        }
    }

    public void setNameColumnName(String nameColumnName) {
        if (nameColumnName != null) {
            getModel().addColumn(new Column<T>(nameColumnName) {

                @Override public Object getValue(final T row) {
                    return Attribute.NAME.get(row);
                }
            });
        }
    }

    public void addColumns(List<? extends Attribute<?, ? extends tef.skelton.Model>> attributes) {
        for (final Attribute<?, ? extends tef.skelton.Model> attribute : attributes) {
            addColumn(attribute);
        }
    }

    public void addColumn(Attribute<?, ? extends tef.skelton.Model> attr) {
        final String columnName = attr.getName();
        getModel().addColumn(new Column<T>(columnName, attr.getType().getJavaType()) {

            @Override public Object getValue(final T row) {
                return formatValue(row.getValue(columnName));
            }
        });
    }

    protected Object formatValue(Object value) {
        return value;
    }

    @Override protected Color getRowBackgroundColor(boolean isSelected, boolean hasFocus, int rowIndex) {
        return (highlightedRowIndex_ != null && highlightedRowIndex_.intValue() == rowIndex)
            ? Color.LIGHT_GRAY
            : null;
    }

    public void setHighlightRow(Integer rowIndex) {
        highlightedRowIndex_ = rowIndex;
    }

    @Override public void reload() {
        Set<EntityDto.Oid> selectedValueOids = new HashSet<EntityDto.Oid>();
        for (T selected : getSelectedValues()) {
            if (selected instanceof EntityDto) {
                selectedValueOids.add(((EntityDto) selected).getOid());
            }
        }

        List<? extends T> rows = getNewRows();

        setRows(rows);

        List<T> newSelectedValues = new ArrayList<T>();
        for (T row : rows) {
            if (row instanceof EntityDto) {
                if (selectedValueOids.contains(((EntityDto) row).getOid())) {
                    newSelectedValues.add(row);
                }
            }
        }
        setSelection(newSelectedValues);
    }

    protected abstract List<? extends T> getNewRows();
}
