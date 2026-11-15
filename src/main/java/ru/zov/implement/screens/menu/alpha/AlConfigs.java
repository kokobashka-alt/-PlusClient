package ru.zov.implement.screens.menu.alpha;

import java.util.ArrayList;
import java.util.List;
import ru.zov.api.file.ConfigFiles;
import ru.zov.api.file.exception.FileProcessingException;
import ru.zov.core.Main;
import ru.zov.core.client.ClientInfoProvider;

/** Config file access for the config window; mirrors what the "config" chat command does. */
final class AlConfigs {
    private AlConfigs() {
    }

    static List<String> list() {
        List<String> names = new ArrayList<>();
        names.addAll(ConfigFiles.list(Main.getInstance().getClientInfoProvider().configsDir()));
        return names;
    }

    static boolean save(String name) {
        String clean = ConfigFiles.normalizeName(name);
        if (clean == null) {
            return false;
        }
        try {
            Main.getInstance().getFileController().saveFile(ConfigFiles.fileName(clean));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean load(String name) {
        String clean = ConfigFiles.normalizeName(name);
        if (clean == null) {
            return false;
        }
        Main main = Main.getInstance();
        ClientInfoProvider info = main.getClientInfoProvider();
        if (!ConfigFiles.file(info.configsDir(), clean).isFile()) {
            return false;
        }
        try {
            main.getFileController().loadFile(ConfigFiles.fileName(clean));
            return true;
        } catch (FileProcessingException | RuntimeException e) {
            return false;
        }
    }
}
