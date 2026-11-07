package ru.zov.api.feature.command.datatypes;

import ru.zov.api.feature.command.exception.CommandException;

public interface IDatatypeFor<T> extends IDatatype {
   T get(IDatatypeContext var1) throws CommandException;
}
