import i18n.Langue;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import vues.GestionnaireDesPolices;
import vues.LookInterface;

import models.outils.Outils;


public class Fenetre_ChoixLangue extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private final int MARGES_PANEL                 = 40;
    
    private static final ImageIcon I_FR     = new ImageIcon("lang/fr_FR.jpg");
    private static final ImageIcon I_EN    = new ImageIcon("lang/en_EN.png");
    
    private JButton bFR = new JButton("Français",new ImageIcon(Outils.redimentionner(I_FR.getImage(), 150, 100)));
    private JButton bEN = new JButton("English",new ImageIcon(Outils.redimentionner(I_EN.getImage(), 150, 100)));
    private JButton bQuitter = new JButton("Quitter");
    
    private JLabel lblTitre = new JLabel("CHOIX DE LA LANGUE");
    
    
    public Fenetre_ChoixLangue()
    {
        super((Frame) null, true);
        
        JPanel pForm = new JPanel(new BorderLayout());
        
        setContentPane(pForm);
        
        
        setTitle("Choix de la langue - ASD Tower Defense");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        pForm.setBorder(new EmptyBorder(new Insets(MARGES_PANEL, MARGES_PANEL,
                MARGES_PANEL, MARGES_PANEL)));
        
        pForm.setBackground(LookInterface.COULEUR_DE_FOND_PRI);

        JPanel pDrapeaux = new JPanel(new FlowLayout());
        pDrapeaux.setOpaque(false);
        
        lblTitre.setFont(GestionnaireDesPolices.POLICE_SOUS_TITRE);
        lblTitre.setForeground(LookInterface.COULEUR_TEXTE_SEC);
        pForm.add(lblTitre,BorderLayout.NORTH);
        
        GestionnaireDesPolices.setStyle(bFR);
        GestionnaireDesPolices.setStyle(bEN);
        GestionnaireDesPolices.setStyle(bQuitter);
        
        pDrapeaux.add(bFR);
        pDrapeaux.add(bEN);
        
        bFR.setVerticalTextPosition(SwingConstants.BOTTOM);
        bEN.setVerticalTextPosition(SwingConstants.BOTTOM);
        
        bFR.setHorizontalTextPosition(SwingConstants.CENTER);
        bEN.setHorizontalTextPosition(SwingConstants.CENTER);
        
        pForm.add(pDrapeaux,BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout());
        p2.setOpaque(false);
        p2.add(bQuitter,BorderLayout.CENTER);
        
        pForm.add(p2,BorderLayout.SOUTH);
        
        
        bFR.addActionListener(this);
        bEN.addActionListener(this);
        bQuitter.addActionListener(this);
        
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        
        if(src == bFR)
        {
            Langue.initaliser("lang/fr_FR.json");
            dispose();
        }
        else if(src == bEN)
        {
            Langue.initaliser("lang/en_EN.json");
            dispose();
        } 
        else
        {
            System.exit(0);
        }
    }
}