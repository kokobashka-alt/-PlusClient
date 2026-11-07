package ru.zov.api.feature.command;

import ru.zov.api.feature.command.argparser.IArgParserManager;

public interface ICommandSystem {
   IArgParserManager getParserManager();
}
