package models.creatures;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Classe de gestion d'une creature.
 * 
 * Les creatures sont des bestioles qui attaque le joueur. L'objectif de celles-ci
 * est simple : ce rendre le plus vite possible (chemin le plus court) d'une zone
 * A a un zone B. Si la creature arrive a survivre jusqu'a la zone B, le joueur 
 * perdra une de ses precieuses vies.
 * 
 * Il existe deux types de creatures, les volantes et les terriennes. Les volantes
 * ne sont pas affecter par les murs et l'emplacement des tours. Elle volent 
 * simplement de la zone A a la zone B sans suivre un chemin particulier.
 * 
 * @author Pierre-Dominique Putallaz
 * @author Aurélien Da Campo
 * @author Lazhar Farjallah
 * @version 1.0 | 27 novemenbre 2009
 * @since jdk1.6.0_16
 */
public abstract class Creature extends Rectangle implements Runnable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * definition des deux types de creature
	 */
	public static final int TYPE_TERRIENNE 	= 0;
	public static final int TYPE_VOLANTE 	= 1;
	private int type = TYPE_TERRIENNE;
	
	/**
	 * chemin actuel de la creature
	 */
	private ArrayList<Point> chemin;
	private int indiceCourantChemin;
	
	/**
	 * sante de la creature, si la sante est <= 0, la creature est morte. 
	 * A ce moment la, elle donne au joueur ses pieces d'or
	 */
	private int sante;
	
	/**
	 * sante maximale de la creature. Utilise pour calculer le pourcentage de 
	 * vie restante de la creature.
	 */
	private int santeMax;
	
	/**
	 * le nombre de pieces d'or que la creature fourni au joueur apres ca mort
	 */
	private int nbPiecesDOr;
	
	/**
	 * La creature gere sont propre thread pour ce deplacer sur le terrain
	 */
	private Thread thread;
	protected boolean enJeu;
	
	/**
	 * vitesse de deplacement de la creature sur le terrain
	 */
	private double vitesse = 10;
	
	/**
	 * permet d'informer d'autres entites du programme lorsque la creature
	 * subie des modifications.
	 */
	private ArrayList<EcouteurDeCreature> ecouteursDeCreature;
	
	
	/**
	 * Constructeur de la creature.
	 * 
	 * @param x la position sur l'axe X de la creature
	 * @param y la position sur l'axe Y de la creature
	 * @param largeur la largeur de la creature
	 * @param hauteur la hauteur de la creature
	 * @param santeMax la sante maximale de la creature
	 * @param nbPiecesDOr le nombre de pieces de la creature
	 */
	public Creature(int x, int y, int largeur, int hauteur, 
					int santeMax, int nbPiecesDOr)
	{
		super(x,y,largeur,hauteur);
		
		this.nbPiecesDOr 	= nbPiecesDOr;
		this.santeMax		= santeMax;
		sante 				= santeMax;
		ecouteursDeCreature = new ArrayList<EcouteurDeCreature>();
	}

	/**
	 * Force les fils de la classe a gerer la copie de la creature.
	 * <p>
	 * Note : cette methode est utilisee lors de la creation d'une vague
	 * de creatures. Au lieu de stocker toutes les creatures de la vague, 
	 * on creer une seule instance de la creature et on la duplique le nombre de
	 * fois souhaite.
	 * 
	 * @return la copie de la creature.
	 */
	abstract public Creature copier();
	
	/**
	 * Permet de recuperer le chemin actuellement suivi par la creature.
	 * @return le chemin actuellement suivi par la creature
	 */
	public synchronized ArrayList<Point> getChemin()
	{
		return chemin;
	}

	/**
	 * Permet de recuperer le type de la creature.
	 * @return le type de la creature
	 */
	public int getType()
	{
		return type;
	}
	
	/**
	 * Permet de recuperer la sante de la creature.
	 * @return la sante de la creature
	 */
	public int getSante()
	{
		return sante;
	}

	/**
	 * Permet de recuperer la sante maximale de la creature.
	 * @return la sante maximale de la creature
	 */
	public int getSanteMax()
	{
		return santeMax;
	}
	
	/**
	 * Permet de recuperer le nombre de pieces d'or de la creature.
	 * @return le nombre de pieces d'or de la creature
	 */
	public int getNbPiecesDOr()
	{
		return nbPiecesDOr;
	}

	/**
	 * Permet de recuperer la position sur l'axe X de la creature
	 * @param x la position sur l'axe X de la creature
	 */
	public void setX(int x)
	{
		this.x = x;
	}
	
	/**
	 * Permet de recuperer la position sur l'axe Y de la creature
	 * @param x la position sur l'axe Y de la creature
	 */
	public void setY(int y)
	{
		this.y = y;
	}

	/**
	 * Permet de modifier le chemin actuel de la creature
	 * @param chemin le nouveau chemin
	 */
	public void setChemin(ArrayList<Point> chemin)
	{
		this.chemin = chemin;
		indiceCourantChemin = 0;
	}
	
	/**
	 * Permet de mettre la creature en jeu.
	 * Cette methode demarre le thread de la creature.
	 */
	public void demarrer()
	{
		thread = new Thread(this);
		thread.start();
		enJeu = true;
	}
	
	/**
	 * Permet de faire avancer le creature sur son chemin.
	 */
	public void avancerSurChemin()
	{
		ArrayList<Point> chemin = getChemin();
		
		if(chemin != null && indiceCourantChemin < chemin.size())
		{
			Point p = chemin.get(indiceCourantChemin);
			
			// avance sur le chemin
			if(x > p.getX()) 	  x--;
			else if(x < p.getX()) x++;
			
			if(y > p.getY()) 	  y--;
			else if(y < p.getY()) y++;
			
			// on a atteint un nouveau noeud du chemin
			if(x == p.getX() && y == p.getY())
				indiceCourantChemin++;
		}
	}
	
	/**
	 * Methode de gestion du thread.
	 * 
	 * Cette methode est appeller par la methode demarrer.
	 */
	public void run()
	{
		// tant que la creature est en jeu et vivante
		while(enJeu && sante >= 0)
		{
			avancerSurChemin();
			
			// TODO a ameliorer, on peut faire mieux
			// le repos du thread defini la vistesse de deplacement de la creature
			try{
				Thread.sleep((long) (1.0/vitesse * 1000.0));
			} 
			catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * Permet de faire subir des degats sur la creature
	 * @param degats les degats recus
	 */
	public void blesser(int degats)
	{
		// diminution de la sante
		sante -= degats;
		
		// appel des ecouteurs de la creature
		for( EcouteurDeCreature edc : ecouteursDeCreature)
			edc.creatureBlessee(this);
		
		// est-elle morte ?
		if(sante <= 0)
			mourrir();
	}
	
	/**
	 * Permet de tuer la creature
	 */
	private void mourrir()
	{
		sante = 0;
		
		// appel des ecouteurs de la creature
		for( EcouteurDeCreature edc : ecouteursDeCreature)
			edc.creatureTuee(this);
	}

	/**
	 * Permet d'ajouter un ecouteur de la creature
	 * @param edc une classe implémentant EcouteurDeCreature
	 */
	public void ajouterEcouteurDeCreature(EcouteurDeCreature edc)
	{
		ecouteursDeCreature.add(edc);
	}
}