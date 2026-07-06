package com.vacances.ravtricount;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataStore {
    private final Path dataFile;

    public DataStore(Path dataFile) {
        this.dataFile = dataFile;
    }

    public AppState load() {
        if (!Files.exists(dataFile)) {
            return new AppState();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile))) {
            Object object = input.readObject();
            if (object instanceof AppState state) {
                return state;
            }
            return new AppState();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Impossible de charger le fichier de données : " + dataFile, e);
        }
    }

    public void save(AppState state) {
        try {
            Path parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                output.writeObject(state);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de sauvegarder le fichier de données : " + dataFile, e);
        }
    }

    public Path getDataFile() {
        return dataFile;
    }
}
