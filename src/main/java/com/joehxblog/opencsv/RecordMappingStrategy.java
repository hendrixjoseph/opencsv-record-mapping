package com.joehxblog.opencsv;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.collections4.ListValuedMap;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.stream.Stream;


public class RecordMappingStrategy<T extends Record> extends HeaderColumnNameMappingStrategy<T> {

    public RecordMappingStrategy(Class<T> type) {
        if (type.getConstructors().length > 0) {
            this.setType(type);
        } else {
            throw new RuntimeException("record needs to have its constructor be public");
        }
    }

    @Override
    public T populateNewBean(String[] line) {
        var constructor = this.type.getConstructors()[0];
        var recordComponents = this.type.getRecordComponents();

        var initArgs = Stream.of(recordComponents)
                .map(recordComponent -> this.createParameter(line, recordComponent))
                .toArray();

        try {
            return (T) constructor.newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void loadUnadornedFieldMap(ListValuedMap<Class<?>, Field> fields) {
        fields.entries().stream()
                .filter(entry -> !(Serializable.class.isAssignableFrom(entry.getKey()) && "serialVersionUID".equals(entry.getValue().getName())))
                .filter(entry -> !entry.getValue().isAnnotationPresent(CsvRecurse.class))
                .forEach(entry -> {
                    var converter = determineConverter(entry.getValue(), entry.getValue().getType(), null, null, null);
                    fieldMap.put(entry.getValue().getName(), new BeanFieldSingleValue<>(
                            entry.getKey(), entry.getValue(),
                            false, errorLocale, converter, null, null));
                });
    }

    @Override
    protected BeanField<T, String> findField(int col) throws CsvBadConverterException {
        BeanField<T, String> beanField = null;

        var columnName = getColumnName(col);

        if (columnName == null) {
            return null;
        }

        columnName = columnName.trim();

        if (!columnName.isEmpty()) {
            beanField = fieldMap.get(columnName);
        }

        return beanField;
    }

    private Object createParameter(String[] line, RecordComponent recordComponent) {
        var index = this.headerIndex.getByName(recordComponent.getName())[0];
        var cell = line[index];

        if (cell.isBlank()) {
            return null;
        } else {

            var type = recordComponent.getType();
            var field = this.findField(index).getField();

            var converter = this.determineConverter(field, type, null, null, null);

            try {
                return converter.convertToRead(cell);
            } catch (CsvDataTypeMismatchException | CsvConstraintViolationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
