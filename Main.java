import java.util.*;

public class Main {

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

enum CellState {
    EMPTY, SHIP, WATER, HIT, SUNK
}

class Coordinate {
    int row;
    int col;

    Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }
}

class Ship {
    List<Coordinate> positions = new ArrayList<>();
    int hits = 0;

    void addPosition(Coordinate c) {
        positions.add(c);
    }

    boolean isSunk() {
        return hits == positions.size();
    }
}

class Board {

    static final int SIZE = 10;
    CellState[][] grid = new CellState[SIZE][SIZE];
    List<Ship> ships = new ArrayList<>();

    Board() {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                grid[i][j] = CellState.EMPTY;
    }

    boolean canPlaceShip(int row, int col, int length, boolean horizontal) {
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);

            if (r < 0 || r >= SIZE || c < 0 || c >= SIZE)
                return false;

            if (grid[r][c] != CellState.EMPTY)
                return false;

            // Abstand prüfen
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                        if (grid[nr][nc] == CellState.SHIP)
                            return false;
                    }
                }
            }
        }
        return true;
    }

    void placeShip(int row, int col, int length, boolean horizontal) {
        Ship ship = new Ship();
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);
            grid[r][c] = CellState.SHIP;
            ship.addPosition(new Coordinate(r, c));
        }
        ships.add(ship);
    }

    String shoot(int row, int col) {
        if (grid[row][col] == CellState.WATER ||
                grid[row][col] == CellState.HIT ||
                grid[row][col] == CellState.SUNK) {
            return "ALREADY";
        }

        if (grid[row][col] == CellState.EMPTY) {
            grid[row][col] = CellState.WATER;
            return "WATER";
        }

        if (grid[row][col] == CellState.SHIP) {
            grid[row][col] = CellState.HIT;

            for (Ship s : ships) {
                for (Coordinate c : s.positions) {
                    if (c.row == row && c.col == col) {
                        s.hits++;
                        if (s.isSunk()) {
                            sinkShip(s);
                            return "SUNK";
                        }
                        return "HIT";
                    }
                }
            }
        }
        return "";
    }

    void sinkShip(Ship s) {
        for (Coordinate c : s.positions) {
            grid[c.row][c.col] = CellState.SUNK;

            // Wasser drum herum markieren
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = c.row + dr;
                    int nc = c.col + dc;
                    if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                        if (grid[nr][nc] == CellState.EMPTY)
                            grid[nr][nc] = CellState.WATER;
                    }
                }
            }
        }
    }

    boolean allShipsSunk() {
        for (Ship s : ships)
            if (!s.isSunk())
                return false;
        return true;
    }

    // ANSI Farben
    static final String RESET = "\u001B[0m";
    static final String BLUE = "\u001B[34m";
    static final String YELLOW = "\u001B[33m";
    static final String RED = "\u001B[31m";
    static final String GREEN = "\u001B[32m";

    void print(boolean hideShips) {
        System.out.print("  ");
        for (char c = 'A'; c <= 'J'; c++)
            System.out.print(c + " ");
        System.out.println();

        for (int i = 0; i < SIZE; i++) {
            System.out.print((i + 1) + (i < 9 ? " " : ""));
            for (int j = 0; j < SIZE; j++) {

                switch (grid[i][j]) {

                    case EMPTY:
                        System.out.print(". ");
                        break;

                    case SHIP:
                        if (hideShips)
                            System.out.print(". ");
                        else
                            System.out.print(GREEN + "S " + RESET);
                        break;

                    case WATER:
                        System.out.print(BLUE + "o " + RESET);
                        break;

                    case HIT:
                        System.out.print(YELLOW + "x " + RESET);
                        break;

                    case SUNK:
                        System.out.print(RED + "# " + RESET);
                        break;
                }
            }
            System.out.println();
        }
    }
}

class Game {

    Board playerBoard = new Board();
    Board botBoard = new Board();
    Scanner scanner = new Scanner(System.in);
    Random random = new Random();

    int[] fleet = {5, 4, 4, 3, 3, 3, 2, 2, 2, 2};

    void start() {
        System.out.println("=== SCHIFFE VERSENKEN ===");
        placePlayerShips();
        placeBotShips();
        gameLoop();
    }

    void placePlayerShips() {
        System.out.println("Platziere deine Schiffe:");
        for (int length : fleet) {
            while (true) {
                playerBoard.print(false);
                System.out.println("Schiff Länge " + length + " platzieren (z.B. A5 H oder A5 V):");
                String input = scanner.nextLine().toUpperCase();

                try {
                    String[] parts = input.split(" ");
                    char colChar = parts[0].charAt(0);
                    int col = colChar - 'A';
                    int row = Integer.parseInt(parts[0].substring(1)) - 1;
                    boolean horizontal = parts[1].equals("H");

                    if (playerBoard.canPlaceShip(row, col, length, horizontal)) {
                        playerBoard.placeShip(row, col, length, horizontal);
                        break;
                    } else {
                        System.out.println("Ungültige Position!");
                    }
                } catch (Exception e) {
                    System.out.println("Falsches Format!");
                }
            }
        }
    }

    void placeBotShips() {
        for (int length : fleet) {
            while (true) {
                int row = random.nextInt(10);
                int col = random.nextInt(10);
                boolean horizontal = random.nextBoolean();

                if (botBoard.canPlaceShip(row, col, length, horizontal)) {
                    botBoard.placeShip(row, col, length, horizontal);
                    break;
                }
            }
        }
    }

    void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    void gameLoop() {

        while (true) {

            clearScreen();

            System.out.println("=== SCHIFFE VERSENKEN ===\n");

            System.out.println("Dein Feld:");
            playerBoard.print(false);

            System.out.println("\nGegnerfeld:");
            botBoard.print(true);

            // Spieler schießt
            while (true) {
                System.out.println("\nSchuss eingeben (z.B. A5):");
                String input = scanner.nextLine().toUpperCase();

                try {
                    int col = input.charAt(0) - 'A';
                    int row = Integer.parseInt(input.substring(1)) - 1;

                    String result = botBoard.shoot(row, col);

                    if (result.equals("ALREADY")) {
                        System.out.println("Bereits beschossen!");
                        continue;
                    }

                    System.out.println("Ergebnis: " + result);
                    break;

                } catch (Exception e) {
                    System.out.println("Ungültige Eingabe!");
                }
            }

            if (botBoard.allShipsSunk()) {
                clearScreen();
                System.out.println("DU GEWINNST!");
                return;
            }

            // Bot schießt
            while (true) {
                int row = random.nextInt(10);
                int col = random.nextInt(10);

                String result = playerBoard.shoot(row, col);

                if (!result.equals("ALREADY")) {
                    System.out.println("\nBot schießt auf "
                            + (char) ('A' + col) + (row + 1)
                            + " -> " + result);
                    break;
                }
            }

            if (playerBoard.allShipsSunk()) {
                clearScreen();
                System.out.println("BOT GEWINNT!");
                return;
            }

            System.out.println("\nDrücke ENTER für nächste Runde...");
            scanner.nextLine();
        }
    }


}