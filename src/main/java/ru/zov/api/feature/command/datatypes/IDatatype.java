package ru.zov.api.feature.command.datatypes;

import java.util.stream.Stream;
import ru.zov.api.feature.command.exception.CommandException;
import ru.zov.common.QuickImports;

public interface IDatatype extends QuickImports {
   Stream<String> tabComplete(IDatatypeContext var1) throws CommandException;
}
