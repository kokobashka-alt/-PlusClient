package ru.zov.api.feature.command.datatypes;

import ru.zov.api.feature.command.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
   T apply(IDatatypeContext var1, O var2) throws CommandException;
}
