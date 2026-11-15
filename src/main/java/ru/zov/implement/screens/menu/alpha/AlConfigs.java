package ru.zov.implement.screens.menu.alpha;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ru.zov.api.file.FileController;
import ru.zov.api.file.FileRepository;
import ru.zov.api.file.exception.FileProcessingException;
import ru.zov.core.Main;
import ru.zov.core.client.ClientInfoProvider;

/** Config file access for the config window; mirrors what the "config" chat command does. */
final class AlConfigs {
    private AlConfigs() {
    }

    static List<String> list() {
        List<String> names = new ArrayList<>();
        File[] files = Main.getInstance().getClientInfoProvider().configsDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    names.add(file.getName().substring(0, file.getName().length() - ".json".length()));
                }
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    static boolean save(String name) {
        try {
            Main.getInstance().getFileController().saveFile(name + ".json");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean load(String name) {
        Main main = Main.getInstance();
        ClientInfoProvider info = main.getClientInfoProvider();
        if (!new File(info.configsDir(), name + ".json").exists()) {
            return false;
        }
        try {
            FileRepository repository = new FileRepository();
            repository.setup(main);
            FileController controller = new FileController(repository.getClientFiles(), info.filesDir(), info.configsDir());
            try {
                controller.loadFile(name + ".json");
            } finally {
                // The controller starts its own auto-save timer; this one is only needed for the single load.
                controller.stopAutoSave();
            }
            return true;
        } catch (FileProcessingException e) {
            return false;
        }
    }
}
