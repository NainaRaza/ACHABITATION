package com.vacances.ravtricount;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                DataStore dataStore = new DataStore(Path.of("rav-tricount-data.ser"));
                AppState state = dataStore.load();
                MainFrame frame = new MainFrame(state, dataStore);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(null, e.getMessage(), "Erreur au démarrage", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
