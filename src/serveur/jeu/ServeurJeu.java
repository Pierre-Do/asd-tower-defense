package serveur.jeu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Observable;
import java.util.Map.Entry;

import models.animations.Animation;
import models.creatures.Creature;
import models.creatures.VagueDeCreatures;
import models.jeu.BadPosException;
import models.jeu.EcouteurDeJeu;
import models.jeu.Jeu;
import models.jeu.NoMoneyException;
import models.jeu.PathBlockException;
import models.joueurs.Joueur;
import models.tours.Tour;
import models.tours.TourArcher;

import reseau.Canal;
import reseau.Port;

/**
 * Cette classe contiendra le serveur de jeu sur lequel se connecteront tout les
 * cliens.
 * 
 * @author Pierre-Do
 * 
 */
public class ServeurJeu extends Observable implements ConstantesServeurJeu,
		EcouteurDeJeu
{
	/**
	 * La version courante du serveur
	 */
	public static final String VERSION = "0.1";

	/**
	 * Le port sur lequel le serveur écoute par defaut
	 */
	public final static int _port = 2357;

	/**
	 * Fanion pour le mode debug
	 */
	private static final boolean DEBUG = false;

	/**
	 * Liste des clients enregistrés sur le serveur
	 */
	private HashMap<Integer, JoueurDistant> clients = new HashMap<Integer, JoueurDistant>();

	/**
	 * Lien vers le module coté serveur du jeu
	 */
	private Jeu serveurJeu;

	/**
	 * Méthode MAIN : entrée dans le programme en cas de lancement en standalone
	 * du serveur
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			// Création d'un serveur de jeu en standalone
			new ServeurJeu(null);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param serveurJeu
	 * @throws IOException
	 */
	public ServeurJeu(Jeu serveurJeu) throws IOException
	{
		// Assignation du serveur
		this.serveurJeu = serveurJeu;
		// Réglage du niveau d'affichage des messages clients
		JoueurDistant.verboseMode = 0;
		// Réservation du port d'écoute
		Port port = new Port(_port);
		port.reserver();
		// Canal d'écoute
		Canal canal;
		// Boucle d'attente de connections
		while (true)
		{
			// On attend qu'un joueur se présente
			log("écoute sur le port " + _port);
			canal = new Canal(port, DEBUG);
			log("Récéption de " + canal.getIpClient());
			int IDClient = canal.recevoirInt();
			log("Nouveau joueur ! ID : " + IDClient);
			enregistrerClient(IDClient, canal);
		}
	}

	private void enregistrerClient(int IDClient, Canal canal)
	{
		// On vérifie que l'ID passé en paramêtre soit bien unique
		if (clients.containsKey(IDClient))
		{
			log("ERROR : Le client " + IDClient + " est déjà dans la partie");
			// On déconnecte le client; // FIXME
			canal.fermer();
		} else
		{
			// On inscrit le joueur à la partie
			clients.put(IDClient, new JoueurDistant(IDClient, canal, this));
			setChanged();
			notifyObservers(clients);
		}
	}

	/**
	 * Affiche toutes les informations de tous les clients connectés.
	 */
	public synchronized void infos()
	{
		System.out.println("Serveur de jeu");
		System.out.println("Nombre de joueurs : " + clients.size());
		for (Entry<Integer, JoueurDistant> joueur : clients.entrySet())
			System.out.println(joueur.getValue());
	}

	protected synchronized static void log(String msg)
	{
		System.out.print("[SERVEUR]");
		System.out.println(msg);
	}

	private synchronized Joueur repererJoueur(int ID)
	{
		for (Joueur joueur : serveurJeu.getJoueurs())
		{
			if (joueur.getId() == ID)
				return joueur;
		}
		return null;
	}

	/**
	 * Envoi l'état de tous les objets à tous les clients
	 */
	public void update()
	{
		// Parcourt de toutes les tours sur le terrain
		for (Tour t : serveurJeu.getTours())
		{
			// Extraction des paramêtres
			int ID = 0;// t.getID(); // FIXME
			int x = (int) t.getX();
			int y = (int) t.getY();
			int etat = 0;// t.getEtat(); // FIXME
			// On envoi les infos à chaque client
			for (Entry<Integer, JoueurDistant> joueur : clients.entrySet())
				joueur.getValue().afficherObjet(ID, x, y, etat);
		}

		// TODO : Créatures
	}

	/**************** NOTIFICATIONS **************/

	@Override
	public void creatureArriveeEnZoneArrivee(Creature creature)
	{
		update();
	}

	@Override
	public void creatureBlessee(Creature creature)
	{
		update();
	}

	@Override
	public void creatureTuee(Creature creature)
	{
		update();
	}

	@Override
	public void etoileGagnee()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void partieTerminee()
	{
		for (Entry<Integer, JoueurDistant> joueur : clients.entrySet())
			joueur.getValue().partieTerminee();
	}

	@Override
	public void vagueEntierementLancee(VagueDeCreatures vague)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void animationAjoutee(Animation animation)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void animationTerminee(Animation animation)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void creatureAjoutee(Creature creature)
	{
		update();
	}

	@Override
	public void joueurAjoute(Joueur joueur)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void partieDemarree()
	{
		// Notification aux joueurs que la partie débutte
		notifyAll();
	}

	@Override
	public void tourAmelioree(Tour tour)
	{
		update();
	}

	@Override
	public void tourPosee(Tour tour)
	{
		update();
	}

	@Override
	public void tourVendue(Tour tour)
	{
		update();
	}

	/**
	 * Supprime un joueur de la partie
	 * 
	 * @param ID
	 *            l'ID du joueur à supprimer
	 */
	public synchronized void supprimerJoueur(int ID)
	{
		clients.remove(ID);
		// TODO
		setChanged();
		notifyObservers(clients);
	}

	/************************** ACTIONS DES JOUEURS ************************/

	/**
	 * 
	 * @param typeVague
	 * @return
	 */
	public synchronized int lancerVague(int IDPlayer, int typeVague)
	{
		log("Le joueur " + IDPlayer + " désire lancer une vague de type"
				+ typeVague);
		return 0;
	}

	/**
	 * 
	 * @param IDJoueur
	 * @param typeTour
	 * @param x
	 * @param y
	 * @return
	 */
	public synchronized int poserTour(int IDJoueur, int typeTour, int x, int y)
	{
		log("Le joueur " + IDJoueur + " veut poser une tour de type "
				+ typeTour);
		// Selection de la tour cible
		Tour tour = null;
		switch (typeTour)
		{
		case 0: // FIXME
			tour = new TourArcher();
			break;
		default:
			log("Tour " + typeTour + " inconnue.");
			return ERROR;
		}
		// Assignation des paramêtres
		tour.x = x;
		tour.y = y;
		tour.setProprietaire(repererJoueur(IDJoueur));
		try
		{
			// Tentative de poser la tour
			serveurJeu.poserTour(tour);
		} catch (NoMoneyException e)
		{
			// Si pas assez d'argent on retourne le code d'erreur correspondant
			return NO_MONEY;
		} catch (BadPosException e)
		{
			// Mauvaise position
			return BAD_POS;
		} catch (PathBlockException e)
		{
			// Chemin bloqué.
			return PATH_BLOCK;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return OK;
	}

	/**
	 * 
	 * @param IDPlayer
	 * @param nouvelEtat
	 * @return
	 */
	public synchronized int changementEtatJoueur(int IDPlayer, int nouvelEtat)
	{
		log("Le joueur " + IDPlayer + " désire passer en état " + nouvelEtat);
		return 0;
	}

	/**
	 * 
	 * @param nouvelEtatPartie
	 * @return
	 */
	public synchronized int changementEtatPartie(int IDPlayer, int nouvelEtat)
	{
		log("Le joueur " + IDPlayer + " désire passer la partie en état "
				+ nouvelEtat);

		switch (nouvelEtat)
		{
		case EN_PAUSE:
			break;
		case EN_JEU:
			break;
		default:
			break;
		}
		return 0;
	}

	/**
	 * 
	 * @param tourCible
	 * @return
	 */
	public synchronized int ameliorerTour(int IDPlayer, int tourCible)
	{
		log("Le joueur " + IDPlayer + " désire améliorer la tour" + tourCible);
		return 0;
	}

	/**
	 * 
	 * @param tourCibleDel
	 * @return
	 */
	public synchronized int supprimerTour(int IDPlayer, int tourCible)
	{
		log("Le joueur " + IDPlayer + " désire supprimer la tour" + tourCible);
		return 0;
	}

	/**
	 * Envoi un message texte à l'ensemble des clients connectés.
	 * 
	 * @param IDFrom
	 *            L'ID de l'expéditeur.
	 * @param message
	 *            Le message à envoyer.
	 */
	public synchronized void direATous(int IDPlayer, String message)
	{
		log("Le joueur " + IDPlayer + " dit : " + message);
		for (Entry<Integer, JoueurDistant> joueur : clients.entrySet())
			joueur.getValue().envoyerMessageTexte(IDPlayer, message);
	}

	/**
	 * Envoi un message texte à un client en particulier.
	 * 
	 * @param IDFrom
	 *            L'ID de l'expéditeur
	 * @param IDTo
	 *            L'ID du destinataire
	 * @param message
	 *            Le message à envoyer.
	 */
	public synchronized void direAuClient(int IDPlayer, int IDTo, String message)
	{
		log("Le joueur " + IDPlayer + " désire envoyer un message à " + IDTo
				+ "(" + message + ")");
		clients.get(IDTo).envoyerMessageTexte(IDPlayer, message);
	}
}