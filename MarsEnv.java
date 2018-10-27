import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {
	
    public static final int GSize = 7; // grid size
    public static final int GARB  = 16; // garbage code in grid model
	
    public static final Term    ns = Literal.parseLiteral("next(slot)");
    public static final Term    pg = Literal.parseLiteral("pick(garb)");
    public static final Term    dg = Literal.parseLiteral("drop(garb)");
    public static final Term    bg = Literal.parseLiteral("burn(garb)");
    public static final Literal g1 = Literal.parseLiteral("garbage(r1)");
    public static final Literal g2 = Literal.parseLiteral("garbage(r2)");
	
	//r3
	public static final Term	mv = Literal.parseLiteral("move(slot)");
	public static final Term	gg = Literal.parseLiteral("generate(garb)");
	
    static Logger logger = Logger.getLogger(MarsEnv.class.getName());
	
    private MarsModel model;
    private MarsView  view;
	
    @Override
    public void init(String[] args) {
        model = new MarsModel();
        view  = new MarsView(model);
        model.setView(view);
        updatePercepts();
    }
	
    @Override
    public boolean executeAction(String ag, Structure action) {
        logger.info(ag+" doing: "+ action);
        try {
            if (action.equals(ns)) {
                model.nextSlot();
            } else if (action.getFunctor().equals("move_towards")) {
                int x = (int)((NumberTerm)action.getTerm(0)).solve();
                int y = (int)((NumberTerm)action.getTerm(1)).solve();
                model.moveTowards(x,y);
            } else if (action.equals(pg)) {
                model.pickGarb();
            } else if (action.equals(dg)) {
                model.dropGarb();
            } else if (action.equals(bg)) {
                model.burnGarb();
            } else if (action.equals(mv)) {
				model.move();
			} else if (action.equals(gg)) {
				model.generate();
			}else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		
        updatePercepts();
		
        try {
            Thread.sleep(200);
        } catch (Exception e) {}
        informAgsEnvironmentChanged();
        return true;
    }
	
    /** creates the agents perception based on the MarsModel */
    void updatePercepts() {
        clearPercepts();
		
        Location r1Loc = model.getAgPos(0);
        Location r2Loc = model.getAgPos(1);
		
        Literal pos1 = Literal.parseLiteral("pos(r1," + r1Loc.x + "," + r1Loc.y + ")");
        Literal pos2 = Literal.parseLiteral("pos(r2," + r2Loc.x + "," + r2Loc.y + ")");
		
        addPercept(pos1);
        addPercept(pos2);
		
        if (model.hasObject(GARB, r1Loc)) {
            addPercept(g1);
        }
        if (model.hasObject(GARB, r2Loc)) {
            addPercept(g2);
        }
    }
	
    class MarsModel extends GridWorldModel {
		
        public static final int MErr = 2; // max error in pick garb
        int nerr; // number of tries of pick garb
		int nerb; // number of tries of burning grab
        boolean r1HasGarb = false; // whether r1 is carrying garbage or not
		
        Random random = new Random(System.currentTimeMillis());
		Random r = new Random();
		
        private MarsModel() {
            super(GSize, GSize, 2);
			int[][] occupied = new int[7][2];
			boolean esta = false;
			int x, y;
            // initial location of agents
            try {
				occupied[6][0] = r.nextInt(GSize);
				occupied[6][1] = r.nextInt(GSize);
                setAgPos(0, occupied[6][0], occupied[6][1]);
                while(!esta){
					x = r.nextInt(GSize);
					y = r.nextInt(GSize);
					if(occupied[6][0] != x || occupied[6][1] != y){
						esta = true;
						occupied[5][0] = x;
						occupied[5][1] = y;
					}
				}
				
                setAgPos(1, occupied[5][0], occupied[5][1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
			
            // initial location of garbage
			for(int i = 0; i < 4; i++){
				esta = true;
				while(esta){
					x = r.nextInt(GSize); y = r.nextInt(GSize);
					if(!check(occupied, x, y, i)){
						occupied[i][0] = x;
						occupied[i][1] = y;
						add(GARB, x, y);
						esta = false;
					}
				}
			}
			
        }
		
		boolean check(int[][] lista, int x, int y, int i){
			for (int j = 0; j < i; j++){
				if(lista[j][0] == x && lista[j][1] == y){
					return true;
				}
			}
			return false;
		}
		
		void nextSlot() throws Exception {  
            Location r1 = getAgPos(0);
			System.out.println(r1);
            
			if(r1.x != 6 || r1.y != 6){
				r1.y++;
				if (r1.y == getHeight()) {
					r1.y = 0;
					r1.x++;
				}
				// finish	ed searching the whole grid
				if (r1.x == getWidth()) {
					return;
				}
			}
			else{
				System.out.println("Hola");
				r1.x = 0;
				r1.y = 0;
			}
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
        }
		
        void moveTowards(int x, int y) throws Exception {
            Location r1 = getAgPos(0);
            if (r1.x < x)
                r1.x++;
            else if (r1.x > x)
                r1.x--;
            if (r1.y < y)
                r1.y++;
            else if (r1.y > y)
                r1.y--;
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
        }
		
        void pickGarb() {
            // r1 location has garbage
            if (model.hasObject(GARB, getAgPos(0))) {
                // sometimes the "picking" action doesn't work
                // but never more than MErr times
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, getAgPos(0));
                    nerr = 0;
                    r1HasGarb = true;
                } else {
                    nerr++;
                }
            }
        }
        void dropGarb() {
            if (r1HasGarb) {
                r1HasGarb = false;
                add(GARB, getAgPos(0));
            }
        }
        void burnGarb() {
            // r2 location has garbage
            if (model.hasObject(GARB, getAgPos(1))) {
				if(random.nextBoolean() || nerb == MErr){
                	remove(GARB, getAgPos(1));
					nerb = 0;
				}else{
					nerb++;
					System.out.println("Soy tonto y fallo");
				}
            }
        }
		
		void move(){
			Location r3 = getAgPos(2);
			System.out.println(r3);
			boolean moved = false;
            while(!moved){
				int dir = r.nextInt(4);
				if (dir == 0 && r3.y != 0){
					r3.y--;
					moved = true;
				}else if (dir == 1 && r3.x != 6){
					r3.x++;
					moved = true;
				}else if (dir == 2 && r3.y != 6){
					r3.y++;	
					moved = true;
				}else if (dir == 3 && r3.x != 0){
					r3.x--;	
					moved = true;
				}
			}
			setAgPos(2, r3);
            setAgPos(0, getAgPos(0));
            setAgPos(1, getAgPos(1)); // just to draw it in the view
		}
		
		void generate(){
			Location r3 = getAgPos(2);
			System.out.println(r3);
			if(r.nextInt(5) == 0){	
				add(GARB, r3.x, r3.y);
			}
		}
		
    }
	
	
	
	
	
	
	
	
    class MarsView extends GridWorldView {
		
        public MarsView(MarsModel model) {
            super(model, "Mars World", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18); // change default font
            setVisible(true);
            repaint();
        }
		
        /** draw application objects */
        @Override
        public void draw(Graphics g, int x, int y, int object) {
            switch (object) {
				case MarsEnv.GARB:
                drawGarb(g, x, y);
                break;
            }
        }
		
        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            String label = "R"+(id+1);
            c = Color.blue;
            if (id == 0) {
                c = Color.yellow;
                if (((MarsModel)model).r1HasGarb) {
                    label += " - G";
                    c = Color.orange;
                }
            }
            super.drawAgent(g, x, y, c, -1);
            if (id == 0) {
                g.setColor(Color.black);
            } else {
                g.setColor(Color.white);
            }
            super.drawString(g, x, y, defaultFont, label);
            repaint();
        }
		
        public void drawGarb(Graphics g, int x, int y) {
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "G");
        }
		
    }
}