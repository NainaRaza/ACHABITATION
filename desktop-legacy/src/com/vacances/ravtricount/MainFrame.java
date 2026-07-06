package com.vacances.ravtricount;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.nio.file.Path;

public class MainFrame extends JFrame {
    private final AppState state;
    private final DataStore dataStore;
    private final BalanceService balanceService = new BalanceService();
    private PersonPanel personPanel;
    private ExpensePanel expensePanel;
    private SummaryPanel summaryPanel;

    public MainFrame(AppState state, DataStore dataStore) {
        super("ACHABITATION - Gestion finance - partage au RAV");
        this.state = state;
        this.dataStore = dataStore;
        buildLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 780);
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());

        personPanel = new PersonPanel(state, this::onDataChanged);
        expensePanel = new ExpensePanel(state, balanceService, this::onDataChanged);
        summaryPanel = new SummaryPanel(state, balanceService);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Personnes", personPanel);
        tabs.addTab("Dépenses", expensePanel);
        tabs.addTab("Résumé", summaryPanel);
        add(tabs, BorderLayout.CENTER);

        Path path = dataStore.getDataFile();
        add(new JLabel("  Données sauvegardées dans : " + path.toAbsolutePath()), BorderLayout.SOUTH);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem saveItem = new JMenuItem("Sauvegarder maintenant");
        saveItem.addActionListener(e -> {
            try {
                dataStore.save(state);
                UiUtils.showInfo(this, "Sauvegarde effectuée.");
            } catch (Exception ex) {
                UiUtils.showError(this, ex);
            }
        });
        JMenuItem quitItem = new JMenuItem("Quitter");
        quitItem.addActionListener(e -> dispose());
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(quitItem);

        JMenu helpMenu = new JMenu("Aide");
        JMenuItem rulesItem = new JMenuItem("Règles de calcul");
        rulesItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Règle principale : toutes les dépenses sont réparties proportionnellement au reste à vivre.\n\n"
                        + "Dépense normale :\n"
                        + "- part générale : personnes présentes à la date ;\n"
                        + "- part viande : personnes présentes non végétariennes ;\n"
                        + "- part alcool : personnes présentes non sans-alcool.\n\n"
                        + "Dépense globale :\n"
                        + "- répartition du montant total entre toutes les personnes actives, sans filtre de date, viande ou alcool.\n\n"
                        + "Mode avancé :\n"
                        + "- répartition du montant total entre les personnes sélectionnées manuellement.",
                "Règles de calcul", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(rulesItem);

        bar.add(fileMenu);
        bar.add(helpMenu);
        return bar;
    }

    private void onDataChanged() {
        try {
            dataStore.save(state);
        } catch (Exception e) {
            UiUtils.showError(this, e);
        }
        refreshAll();
    }

    private void refreshAll() {
        personPanel.refresh();
        expensePanel.refresh();
        summaryPanel.refresh();
    }
}
