package reseau.jeu.client;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import reseau.CanalTCP;
import reseau.CanalException;
import reseau.jeu.serveur.ConstantesServeurJeu;
import reseau.jeu.serveur.Protocole;
import models.animations.GainDePiecesOr;
import models.creatures.*;
import models.jeu.Jeu_Client;
import models.joueurs.EmplacementJoueur;
import models.joueurs.Equipe;
import models.joueurs.Joueur;
import models.terrains.Terrain;
import models.tours.*;
import exceptions.*;
import org.json.*;

/**
 * Classe de gestion d'un client de communication réseau.
 * 
 * @author Romain Poulain
 * @author Da Campo Aurélien
 */
public class ClientJeu implements ConstantesServeurJeu, Runnable{
	
    /**
     * Canal de ping-pong envoie / reception
     */
	private CanalTCP canalPingPong;
	
	/**
	 * Canal d'écoute du serveur
	 */
	private CanalTCP canalAsynchrone;
	
	/**
	 * Le jeu du client
	 */
	private Jeu_Client jeu;
	
	/**
	 * Mode verbeux
	 */
    private final boolean verbeux = true;
    
    /**
     * Ecouteur de client jeu pour les notifications
     */
    private EcouteurDeClientJeu edcj;
    
    
    
    /**
     * Constructeur
     * 
     * @param jeu
     * @param IPServeur
     * @param portServeur
     * @param pseudo
     * @throws ConnectException
     * @throws CanalException
     * @throws AucunEmplacementDisponibleException 
     */
	public ClientJeu(Jeu_Client jeu)                
	{
		this.jeu = jeu;
	}
	
	public void etablirConnexion(String IP, int port) 
	    throws ConnectException, CanalException, AucunEmplacementDisponibleException 
    {
       
	    // création du canal 1 (Requête / réponse)
        canalPingPong = new CanalTCP(IP, port, true);
        
        // demande de connexion au serveur (canal 1)
        canalPingPong.envoyerString(jeu.getJoueurPrincipal().getPseudo());
        
        // le serveur nous retourne notre identificateur
        JSONObject msg;
        
        try{
            msg = new JSONObject(canalPingPong.recevoirString());
            receptionInitialisationJoueur(msg);
        } 
        catch (JSONException e1){
            e1.printStackTrace(); 
        } 

        // FIXME
        /*
        try{
            jeu.ajouterJoueur(joueur);  
        } 
        catch (JeuEnCoursException e){e.printStackTrace();} 
        catch (AucunePlaceDisponibleException e){e.printStackTrace();}
        */
        
        
        // reception de la version du serveur
        String version = canalPingPong.recevoirString();
        log("Version du jeu : "+version);
        
        // reception du port du canal 2
        int portCanal2 = canalPingPong.recevoirInt();
        
        // création du canal 2 (Reception asynchrone)
        canalAsynchrone = new CanalTCP(IP, portCanal2, true);

        // lancement de la tache d'écoute du canal 2
        (new Thread(this)).start();
    }
	
	

    /**
	 * Envoyer un message chat
	 * 
	 * TODO controler les betises de l'expediteur (guillemets, etc..)
	 * 
	 * @param message le message
	 * @param cible le joueur visé
     * @throws CanalException 
	 */
	public void envoyerMessage(String message, int cible) throws CanalException
	{
	    canalPingPong.envoyerString(Protocole.construireMsgChat(message, cible));
	}
	
	/**
	 * Permet d'envoyer une vague de créatures
	 * 
	 * @param nbCreatures le nombre de créatures
	 * @param typeCreature le type des créatures
	 * @throws ArgentInsuffisantException 
	 * @throws CanalException 
	 */
	public void envoyerVague(VagueDeCreatures vague) throws ArgentInsuffisantException, CanalException
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put("TYPE", VAGUE);
			json.put("TYPE_CREATURE", TypeDeCreature.getTypeCreature(vague.getNouvelleCreature()));
			json.put("NB_CREATURES", vague.getNbCreatures());
			
			canalPingPong.envoyerString(json.toString());
			
			// attente de la réponse
            String resultat = canalPingPong.recevoirString();
            JSONObject resultatJSON = new JSONObject(resultat);
            switch(resultatJSON.getInt("STATUS"))
            {
                case ARGENT_INSUFFISANT :
                    throw new ArgentInsuffisantException("Pas assez d'argent");
                case JOUEUR_INCONNU :
                    logErreur("Joueur inconnu"); // TODO
            }	
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Permet de demander au serveur de jeu la pose d'une tour
	 * 
	 * @param tour la tour a poser
	 * 
	 * @throws ArgentInsuffisantException si pas assez d'argent
	 * @throws ZoneInaccessibleException si la pose est impossible 
	 * @throws CanalException 
	 */
	public void demanderCreationTour(Tour tour) 
	    throws ArgentInsuffisantException, ZoneInaccessibleException, CanalException
	{
		try 
		{
		    // envoye de la requete d'ajout
		    JSONObject json = new JSONObject();
			json.put("TYPE", TOUR_AJOUT);
			json.put("X", tour.x);
			json.put("Y", tour.y);
			json.put("TYPE_TOUR", TypeDeTour.getTypeDeTour(tour));
			
            log("Envoye d'une demande de pose d'une tour");
			
			canalPingPong.envoyerString(json.toString());
			
			// attente de la réponse
			String resultat = canalPingPong.recevoirString();
			JSONObject resultatJSON = new JSONObject(resultat);
			switch(resultatJSON.getInt("STATUS"))
			{
			    case ARGENT_INSUFFISANT :
			        throw new ArgentInsuffisantException("Pas assez d'argent");
			    case ZONE_INACCESSIBLE :
                    throw new ZoneInaccessibleException("Zone non accessible");
			    case CHEMIN_BLOQUE :
                    throw new ZoneInaccessibleException("La tour bloque le chemin");
			}
		} 
		catch (JSONException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Permet de demander au serveur de jeu d'améliorer une tour
	 * 
	 * @param tour la tour
	 * @throws ArgentInsuffisantException
	 * @throws ActionNonAutoriseeException 
	 * @throws CanalException 
	 */
	public void demanderAmeliorationTour(Tour tour) throws ArgentInsuffisantException, ActionNonAutoriseeException, CanalException
	{
		try 
		{
		    // envoye de la requete de vente
		    JSONObject json = new JSONObject();
			json.put("TYPE", TOUR_AMELIORATION);
			json.put("ID_TOWER", tour.getId());
			
			log("Envoye d'une demande de vente d'une tour");
                
			canalPingPong.envoyerString(json.toString());
			
			String resultat = canalPingPong.recevoirString(); // pas d'erreur possible...

			JSONObject resultatJSON = new JSONObject(resultat);
            switch(resultatJSON.getInt("STATUS"))
            {
                case TOUR_INCONNUE: // TODO CHECK
                    throw new NullPointerException("Tour inconnue");
                case ARGENT_INSUFFISANT :
                    throw new ArgentInsuffisantException("Pas assez d'argent");
                case ACTION_NON_AUTORISEE :
                    throw new ActionNonAutoriseeException("Vous n'êtes pas propriétaire");
            }
			
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
     * Permet de demander au serveur de jeu la vente d'une tour
     * 
     * @param tour la tour a vendre
	 * @throws ActionNonAutoriseeException 
	 * @throws CanalException 
     */
	public void demanderVenteTour(Tour tour) throws ActionNonAutoriseeException, CanalException
	{
		try 
		{
			// demande
		    JSONObject json = new JSONObject();
			json.put("TYPE", TOUR_SUPRESSION);
			json.put("ID_TOWER", tour.getId());
			canalPingPong.envoyerString(json.toString());
			
			// reponse
			String resultat = canalPingPong.recevoirString();
            JSONObject resultatJSON = new JSONObject(resultat);
            switch(resultatJSON.getInt("STATUS"))
            {
                case ACTION_NON_AUTORISEE :
                    throw new ActionNonAutoriseeException("Vous n'êtes pas propriétaire");
            }
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
     * Analyse d'un message d'ajout d'une tour
     * 
     * @param message le message
     * @throws TypeDeTourInvalideException 
     */
	private void receptionAjoutTour(JSONObject message) throws JSONException
	{
	   
	    log("Réception d'un objet de type : Tour.");
	    
	    Tour tour = null;

	    // création de la tour en fonction de son type
        try
        {
            int typeDeCreature = message.getInt("TYPE_TOUR");
            
            tour = TypeDeTour.getTour(typeDeCreature);
            
            // initialisation des tours
            tour.x = message.getInt("X");
            tour.y = message.getInt("Y");
            tour.setId(message.getInt("ID_TOUR"));
            
            jeu.poserTourDirect(tour);  
        } 
        catch (TypeDeTourInvalideException e)
        {
            logErreur("Tour inconnue");
        }
	}

	/**
	 * Tache de gestion du canal de réception des messages asynchrones
	 */
	public void run() 
	{
	    while(true)
            try
            {
                attendreMessageCanalAsynchrone();
            } 
	        catch (CanalException e)
            {
                e.printStackTrace();
                return;
            }
	}

	private void attendreMessageCanalAsynchrone() throws CanalException
    {
	    try{  
    	    
	        //log("Attente d'un String sur le canal 2...");
            
    	    JSONObject resultat = new JSONObject(canalAsynchrone.recevoirString());
            
            //log("Canal 2 recoit : "+resultat);
            
            switch(resultat.getInt("TYPE"))
            {
                
                // PARTIE
                case PARTIE_ETAT : 
                    receptionEtatPartie(resultat);
                break; 
            
                // JOUEURS
                case JOUEUR_ETAT :    
                    receptionEtatJoueur(resultat);
                break; 
                
                case JOUEURS_ETAT :    
                    receptionEtatJoueurs(resultat);
                break;
    
                // TOURS
                case TOUR_AJOUT :
                    receptionAjoutTour(resultat);
                    break;
                
                case TOUR_AMELIORATION :
                    receptionAmeliorationTour(resultat);
                    break;    
                    
                case TOUR_SUPRESSION :
                    receptionVendreTour(resultat);
                    break;
                    
                // CREATURES
                case CREATURE_AJOUT :    
                    receptionAjoutCreature(resultat);
                    break;
    
                case CREATURE_ETAT :    
                    receptionEtatCreature(resultat);
                    break;
                    
                case CREATURE_SUPPRESSION :    
                    receptionSuppressionCreature(resultat);
                    break;  
    
                case CREATURE_ARRIVEE : 
                    receptionCreatureArrivee(resultat);
                    break;
                    
                default :
                    logErreur("Réception d'un objet de type : Inconnu.");      
            }
        } 
        catch (JSONException jsone) {
            jsone.printStackTrace();
        }
    }

    private void receptionEtatJoueurs(JSONObject resultat) throws JSONException
    {
        JSONArray joueurs = resultat.getJSONArray("JOUEURS");
        
        jeu.viderEquipes();
        
        Joueur joueur;
        for (int i = 0; i < joueurs.length(); i++)
        {
            JSONObject JSONjoueur = (JSONObject) joueurs.get(i);
            
            int idJoueur        = JSONjoueur.getInt("ID_JOUEUR");
            String nomJoueur    = JSONjoueur.getString("NOM_JOUEUR");
            int idEquipe        = JSONjoueur.getInt("ID_EQUIPE");
            int idEmplacement   = JSONjoueur.getInt("ID_EMPLACEMENT");

            // création du joueur
            joueur = new Joueur(nomJoueur);
            joueur.setId(idJoueur);
            
            // le joueur avec le meme id que l'ancien joueur principal
            // devient le joueur principal
            if(jeu.getJoueurPrincipal().getId() == idJoueur)
                jeu.setJoueurPrincipal(joueur);
               
            // ajout dans l'equipe
            try
            {
                jeu.getEquipe(idEquipe).ajouterJoueur(joueur,jeu.getEmplacementJoueur(idEmplacement));
            } 
            catch (EmplacementOccupeException e)
            {
                // ca n'arrivera pas, les infos viennent du serveur
                e.printStackTrace();
            }
        }
        
        if(edcj != null)
            edcj.joueursMisAJour();
    }

    private void receptionEtatPartie(JSONObject resultat) throws JSONException
    {
        switch(resultat.getInt("ETAT"))
        {
            case PARTIE_INITIALISEE :
                log("Partie initialisee");
                jeu.initialiser();
                break;
            
            case PARTIE_LANCEE :
                jeu.demarrer();
                log("Partie lancee");
                break;
        }
    }

    private void receptionCreatureArrivee(JSONObject message) throws JSONException
    {
	    log("Réception de l'arrivée d'une créature");
	    
        int idCreature = message.getInt("ID_CREATURE");
        jeu.supprimerCreatureDirect(idCreature);
    }

    private void receptionInitialisationJoueur(JSONObject message) throws JSONException, AucunEmplacementDisponibleException
    {
	    log("Réception des donnees d'initialisation du joueur");
        
        
        switch(message.getInt("STATUS"))
        {
            case OK :
                
                int idJoueur = message.getInt("ID_JOUEUR");
                int idEquipe = message.getInt("ID_EQUIPE");
                int idEmplacement = message.getInt("ID_EMPLACEMENT");
                String nomFichierTerrain = message.getString("NOM_FICHIER_TERRAIN");
                 
                log("Reception d'une initialisation de joueur [idJ:"+idJoueur+" idE:"+idEquipe+" idEJ:"+idEmplacement+" terrain:"+nomFichierTerrain+"]");
                
                Terrain terrain;
                
                try 
                {
                    terrain = Terrain.charger(new File("maps/"+nomFichierTerrain));
                    jeu.setTerrain(terrain);
                    terrain.setJeu(jeu);
                    
                    jeu.getJoueurPrincipal().setId(idJoueur);
                    
                    Equipe equipe = jeu.getEquipe(idEquipe);
                    EmplacementJoueur emplacementJoueur = jeu.getEmplacementJoueur(idEmplacement);

                    if(equipe != null && emplacementJoueur != null)
                    {
                        equipe.ajouterJoueur(jeu.getJoueurPrincipal(),emplacementJoueur);  
                        
                        if(edcj != null)
                            edcj.joueurInitialise();  
                    }
                    else
                        logErreur("Equipe ou emplacementJoueur inconnu");
                    
                } 
                catch (IOException e) {
                    logErreur("Terrain inconnu");
                } 
                catch (ClassNotFoundException e) {
                    logErreur(e.getMessage());
                }
                catch (ClassCastException e)
                {
                    logErreur("Format de terrain erroné");
                } 
                catch (EmplacementOccupeException e)
                {
                    // ca n'arrivera pas, l'info vient du serveur
                    logErreur("Emplacement occupé");
                }
 
                break;
        
            case PAS_DE_PLACE :    
                log("Reception d'un refu");
                throw new AucunEmplacementDisponibleException("Aucun emplacement disponible");
        } 
    }
	
	 /**
     * Analyse d'un message d'état d'un joueur
     * 
     * @param message le message
     */
	private void receptionEtatJoueur(JSONObject message) throws JSONException
    {
        int idJoueur = message.getInt("ID_JOUEUR");
        
        log("Réception de l'état d'un joueur (id:"+idJoueur+")");
        
        int nbPiecesDOr = message.getInt("NB_PIECES_OR");
        int score = message.getInt("SCORE");
        int nbViesRestantes = message.getInt("NB_VIES_RESTANTES_EQUIPE");
        
        Joueur joueur = jeu.getJoueur(idJoueur);
        
        if(joueur != null)
        { 
            joueur.setNbPiecesDOr(nbPiecesDOr);
            joueur.setScore(score);
            joueur.getEquipe().setNbViesRestantes(nbViesRestantes);
        }
        else
            logErreur("Joueur inconnu (id:"+idJoueur+")");
    }

    /**
     * Analyse d'un message d'amélioration d'une tour
     * 
     * @param message le message
     */
    private void receptionAmeliorationTour(JSONObject message) throws JSONException
    {
        log("Réception de l'amélioration d'une tour");
        
        int idTour = message.getInt("ID_TOUR");
        jeu.ameliorerTourDirect(idTour);  
    }

    /**
     * Analyse d'un message de vente d'une tour
     * 
     * @param message le message
     */
    private void receptionVendreTour(JSONObject message) throws JSONException
    {
        log("Réception de la suppression d'une tour");
        
        int idTour = message.getInt("ID_TOUR");
        jeu.supprimerTourDirect(idTour); 
    }
    
    /**
     * Analyse d'un message d'ajout d'une créature
     * 
     * @param message le message
     */
    private void receptionAjoutCreature(JSONObject message) throws JSONException
    {
        log("Réception d'un l'ajout d'une créature.");
        
        int id = message.getInt("ID_CREATURE");
        int typeCreature = message.getInt("TYPE_CREATURE");
        
        int x = message.getInt("X");
        int y = message.getInt("Y");
        long santeMax = message.getLong("SANTE_MAX");
        int nbPiecesDOr = message.getInt("NB_PIECES_OR");
        double vitesse = message.getDouble("VITESSE");
        
        Creature creature = TypeDeCreature.getCreature(typeCreature);
        
        if(creature != null)
        {
            creature.setId(id);
            creature.setX(x);
            creature.setY(y);
            
            creature.setSanteMax(santeMax);
            creature.setNbPiecesDOr(nbPiecesDOr);
            creature.setVitesse(vitesse);
            
            
            jeu.ajouterCreatureDirect(creature);
        }
        else
        {
            logErreur("Créature inconnue");
        }
    }
    
    /**
     * Analyse d'un message d'état d'une créature
     * 
     * @param message le message
     */
    private void receptionEtatCreature(JSONObject message) throws JSONException
    {
        int id = message.getInt("ID_CREATURE");
        int x = message.getInt("X");
        int y = message.getInt("Y");
        int sante = message.getInt("SANTE");
        double angle = message.getDouble("ANGLE");
        
        Creature creature = jeu.getCreature(id);
        
        // Elle peut avoir été détruite entre-temps.
        if(creature != null)
        {
            creature.x = x;
            creature.y = y;
            creature.setSante(sante);
            creature.setAngle(angle);
        }
    }
    
    /**
     * Analyse d'un message de suppression d'une creature
     * 
     * @param message le message
     */
    private void receptionSuppressionCreature(JSONObject message) throws JSONException
    {
        int id = message.getInt("ID_CREATURE");
        
        // TODO A REVOIR... pas sur que ca doit ici.
        Creature creature = jeu.getCreature(id);
        
        if(creature != null)
            jeu.ajouterAnimation(new GainDePiecesOr((int)creature.getCenterX(),
                                                (int)creature.getCenterY(),
                                                creature.getNbPiecesDOr()));
        
        jeu.supprimerCreatureDirect(id); 
    }
    
    /**
     * Permet d'afficher des message log
     * 
     * @param msg le message
     */
    public void log(String msg)
    {
        if(verbeux)
            System.out.println("[CLIENT][JOUEUR "+jeu.getJoueurPrincipal().getId()+"] "+msg);
    }
    
    /**
     * Permet d'afficher des message log d'erreur
     * 
     * @param msg le message
     */
    private void logErreur(String msg)
    {
        System.err.println("[CLIENT ERREUR][JOUEUR "+jeu.getJoueurPrincipal().getId()+"] "+msg);
    }



    public void demanderChangementEquipe(Equipe equipe) throws AucunEmplacementDisponibleException, CanalException
    {
        try 
        {
            // envoye de la requete de vente
            JSONObject json = new JSONObject();
            json.put("TYPE", JOUEUR_CHANGER_EQUIPE);
            json.put("ID_EQUIPE", equipe.getId());
            
            log("Envoye d'une demande de changement d'équipe");
                
            canalPingPong.envoyerString(json.toString());
            
            String resultat = canalPingPong.recevoirString();
            JSONObject resultatJSON = new JSONObject(resultat);
            switch(resultatJSON.getInt("STATUS"))
            {
                case PAS_DE_PLACE :
                    throw new AucunEmplacementDisponibleException("Pas de place dans cette équipe");
            }
        } 
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
     
    public void setEcouteurDeClientJeu(EcouteurDeClientJeu edcj)
    {
        this.edcj = edcj;
    }
}