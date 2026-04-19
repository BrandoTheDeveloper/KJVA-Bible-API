package com.zioneltechnology.kjva_bible_api.config;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.mapping.UserDefinedObjectType;

import java.util.ArrayList;
import java.util.List;

public class CustomColumnOrderingStrategy implements ColumnOrderingStrategy {

    private static final List<String> COLUMN_ORDER = List.of(
            "uid",
            "section",
            "category",
            "book",
            "chapter",
            "verse",
            "modern_text",
            "original_text"
    );

    @Override
    public List<Column> orderTableColumns(Table table, Metadata metadata) {
        List<Column> columns = new ArrayList<>(table.getColumns());
        columns.sort((c1, c2) -> {
            int rank1 = getRank(c1.getName());
            int rank2 = getRank(c2.getName());
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
            return c1.getName().compareToIgnoreCase(c2.getName());
        });
        return columns;
    }

    @Override
    public List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata) {
        return null; // use default
    }

    @Override
    public List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType userDefinedType, Metadata metadata) {
        return null; // use default
    }

    @Override
    public void orderTemporaryTableColumns(List<TemporaryTableColumn> temporaryTableColumns, Metadata metadata) {
        // use default
    }

    private int getRank(String columnName) {
        int index = COLUMN_ORDER.indexOf(columnName.toLowerCase());
        return index != -1 ? index : 999;
    }
}
