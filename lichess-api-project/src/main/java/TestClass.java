import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class TestClass {

    public static void main(String[] args) {
        // Settings
        String moveList = "";
        String ratingList = "2000,2200";
        String speedList = "rapid,blitz";
        String variant = "standard";
        boolean showPercentages = true;

        boolean run = true;
        boolean firstMove = moveList.isEmpty();
        boolean whitesMoveFirst = !(moveList.length() % 2 == 0);
        
        if (firstMove) {
        	whitesMoveFirst = true;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to EchoChess!\n");

            while (run) {
                String color = getUserInput(scanner, "Want to play white or black? )>").toLowerCase();

                if (color.equals("exit")) {
                    System.out.println("Program ended.");
                    break;
                }

                if (color.equals("white") || color.equals("w")) {
                    moveList = handleFirstMove(scanner, moveList, whitesMoveFirst, firstMove, ratingList, speedList, variant, showPercentages);
                } else if (color.equals("black") || color.equals("b")) {
                    moveList = handleFirstMove(scanner, moveList, !whitesMoveFirst, firstMove, ratingList, speedList, variant, showPercentages);
                } else {
                    System.out.println("Invalid choice, please type 'white' or 'black'.");
                    continue;
                }

                // Game loop
                while (run) {
                    String playerMove = getUserInput(scanner, "What move do you want to play? )>");

                    if (playerMove.equalsIgnoreCase("exit")) {
                        System.out.println("Program ended.");
                        run = false;
                        break;
                    }

                    String newMoveList = moveList + "," + playerMove;
                    String botMove = pickMove(newMoveList, ratingList, speedList, variant, showPercentages);

                    if ("end".equals(botMove)) {
                        run = false;
                        break;
                    }

                    if (!"invalid".equals(botMove)) {
                        moveList = newMoveList;
                    }
                }
            }
        }
    }

    private static String handleFirstMove(Scanner scanner, String moveList, boolean isFirst, boolean firstMove, 
                                          String ratingList, String speedList, String variant, boolean showPercentages) {
        if (isFirst) {
            String playerMove = getUserInput(scanner, "Pick the first move! )>");
            System.out.println();

            if (playerMove.equalsIgnoreCase("exit")) {
                System.out.println("Program ended.");
                System.exit(0);
            }

            moveList = firstMove ? playerMove : moveList + "," + playerMove;
        }

        String botMove = pickMove(moveList, ratingList, speedList, variant, showPercentages);

        if (!"end".equals(botMove) && !"invalid".equals(botMove)) {
            return moveList + "," + botMove;
        }

        return moveList;
    }

    private static String getUserInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String pickMove(String chosenMoves, String chosenRatings, String chosenSpeeds, String chosenVariants, boolean showPercentages) {
        try {
            String apiUrl = String.format("https://explorer.lichess.ovh/lichess?variant=%s&speeds=%s&ratings=%s&play=%s",
                    chosenVariants, chosenSpeeds, chosenRatings, chosenMoves);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray moves = jsonResponse.getJSONArray("moves");

            if (moves.isEmpty()) {
                System.out.println("No more positions found.");
                return "end";
            }

            List<MoveData> moveList = new ArrayList<>();
            int totalGames = 0;

            for (int i = 0; i < moves.length(); i++) {
                JSONObject move = moves.getJSONObject(i);
                int games = move.getInt("white") + move.getInt("draws") + move.getInt("black");
                totalGames += games;
                moveList.add(new MoveData(move.getString("san"), move.getString("uci"), games));
            }

            if (showPercentages) {
                System.out.println("Moves and their probabilities:");
                for (MoveData move : moveList) {
                    double percentage = (move.games * 100.0) / totalGames;
                    System.out.printf("Move: %s (UCI: %s) - %.2f%% chance\n", move.san, move.uci, percentage);
                }
                System.out.println();
            }

            MoveData selectedMove = selectMoveWeighted(moveList, totalGames);
            System.out.printf("Selected Move: %s (UCI: %s)\n", selectedMove.san, selectedMove.uci);
            return selectedMove.uci;

        } catch (JSONException e) {
            System.out.println("Invalid move. Please use UCI notation (e.g., e2e4).");
            return "invalid";
        } catch (Exception e) {
            System.err.println("Error retrieving move: " + e.getMessage());
            return null;
        }
    }

    static class MoveData {
        String san;
        String uci;
        int games;

        public MoveData(String san, String uci, int games) {
            this.san = san;
            this.uci = uci;
            this.games = games;
        }
    }

    public static MoveData selectMoveWeighted(List<MoveData> moves, int totalGames) {
        double randomValue = new Random().nextDouble() * totalGames;
        double cumulativeWeight = 0;

        for (MoveData move : moves) {
            cumulativeWeight += move.games;
            if (randomValue < cumulativeWeight) {
                return move;
            }
        }
        return moves.get(moves.size() - 1);
    }
}
