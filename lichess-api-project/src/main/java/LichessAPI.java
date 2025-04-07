import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class LichessAPI {
	
	public static void main(String[] args) {
		
		
		
		
		
		//Settings
		String moveList = "";
		String RatingList = "2000,2200";
		String SpeedList = "rapid";
		String variant = "standard";
		boolean showPercentages = true;
		
		
		
		
		
		String pMove = null;
		String saveMoves;
		String colour;
		boolean run = true;
		boolean firstMove = true;
		boolean whitesMoveFirst;
		
		try (Scanner in = new Scanner(System.in)) {
		
			System.out.println("Welcome to EchoChess!");
			System.out.println("Type 'exit' at any time to end the program\n");
			
			//System.out.print("Select rating ranges. Tip: Each value will contain all players from that Lichess ELO to the next value (0, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2500)\nType 'done' after selecting rating ranges to confirm\n)>");
			
			
			if (moveList.length() == 0) {
				whitesMoveFirst = true;
			} else if (moveList.length() % 2 == 0) {
				whitesMoveFirst = false;
				firstMove = false;
			} else {
				whitesMoveFirst = true;
				firstMove = false;
			}
			
			
			outerLoop:
			while (true) {
			
				System.out.print("Want to play white or black? )>");
				colour = in.nextLine().trim();
				System.out.println();
				
				if (colour.equalsIgnoreCase("exit")) {
					System.out.print("Program ended");
					run = false;
					break;
				} else if (colour.equalsIgnoreCase("white") | colour.equalsIgnoreCase("w")) {
					saveMoves = moveList;
					while (whitesMoveFirst) {
						System.out.print("Pick the first move! )>");
						pMove = in.nextLine().trim();
						System.out.println();
						if (pMove.equalsIgnoreCase("exit")) {
							System.out.print("Program ended");
							run = false;
							break outerLoop;
						} else if(pMove.length() != 4) {
							pMove = "invalid";
							System.out.println("Invalid move, please use UCI notation (e.g., e2e4)");
						} else {
							if (firstMove) {
								moveList += pMove;
							} else {
								moveList += "," + pMove;
							}
							pMove = pickMove(moveList, RatingList, SpeedList, variant, showPercentages);
						}
						if (pMove.equals("end")) {
							run = false;
							break outerLoop;
						} else if (!pMove.equals("invalid")) {
							firstMove = false;
							break outerLoop;
						}
						moveList = saveMoves;
					}
					if (!whitesMoveFirst) {
						pMove = pickMove(moveList, RatingList, SpeedList, variant, showPercentages);
						break outerLoop;
					}
				} else if (colour.equalsIgnoreCase("black") | colour.equalsIgnoreCase("b")) {
					if (whitesMoveFirst) {
						pMove = pickMove(moveList, RatingList, SpeedList, variant, showPercentages);
						break;
					} else {
						saveMoves = moveList;
						while (true) {
							System.out.print("Pick the first move! )>");
							pMove = in.nextLine().trim();
							System.out.println();
							if (pMove.equalsIgnoreCase("exit")) {
								System.out.print("Program ended");
								run = false;
								break outerLoop;
							} else if(pMove.length() != 4) {
								pMove = "invalid";
								System.out.println("Invalid move, please use UCI notation (e.g., e2e4)");
							} else {
								moveList += "," + pMove;
								pMove = pickMove(moveList, RatingList, SpeedList, variant, showPercentages);
							}
							if (pMove.equals("end")) {
								run = false;
								break outerLoop;
							} else if (!pMove.equals("invalid")) {
								break outerLoop;
							}
							moveList = saveMoves;
						}
					}
				} else {
					System.out.println("Invalid answer, please type 'white' or 'black'");
				}
			}
			outerLoop:
			while (run) {
				
				if (firstMove) {
					if (pMove.equals("end")) {
						break;
					} else {
						moveList += pMove;
						firstMove = false;
					}
				} else {
					if (pMove.equals("end")) {
						break;
					} else {
						moveList += "," + pMove;
					}
				}
				
				saveMoves = moveList;
				
				while(true) {
					
					System.out.print("What move do you want to play? )>");
					pMove = in.nextLine().trim();
					System.out.println();
					
					if (pMove.equalsIgnoreCase("exit")) {
						System.out.print("Program ended");
						break outerLoop;
					} else if(pMove.length() != 4) {
						System.out.println("Invalid move, please use UCI notation (e.g., e2e4)");
						pMove = "invalid";
					} else {
						moveList += "," + pMove;
						pMove = pickMove(moveList, RatingList, SpeedList, variant, showPercentages);
					}
					
					if (!pMove.equals("invalid")) {
						break;
					} else {
						moveList = saveMoves;
					}
					
					
				}
			}
		}
		
	}
	
    public static String pickMove(String chosenMoves, String chosenRatings, String chosenSpeeds, String chosenVariants, boolean showPercentages) {
        try {
            String apiUrl = "https://explorer.lichess.ovh/lichess?variant=" + chosenVariants + "&speeds=" + chosenSpeeds + "&ratings=" + chosenRatings + "&play=" + chosenMoves;

            URI uri = new URI(apiUrl);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();

            JSONObject jsonResponse = new JSONObject(responseBody);
            
            JSONArray moves = jsonResponse.getJSONArray("moves");
            
            int totalGames = 0;
            List<MoveData> moveList = new ArrayList<>();

            for (int i = 0; i < moves.length(); i++) {
                JSONObject move = moves.getJSONObject(i);
                totalGames += move.getInt("white") + move.getInt("draws") + move.getInt("black");
            }

            for (int i = 0; i < moves.length(); i++) {
                JSONObject move = moves.getJSONObject(i);
                int whiteGames = move.getInt("white");
                int drawGames = move.getInt("draws");
                int blackGames = move.getInt("black");

                int moveGames = whiteGames + drawGames + blackGames;

                double movePercentage = ((double) moveGames / totalGames) * 100;
                
                moveList.add(new MoveData(move.getString("san"), move.getString("uci"), movePercentage));
            }

            if (showPercentages) {
	            System.out.println("Moves and their percentages:");
	            for (MoveData move : moveList) {
	                System.out.printf("Move: %s (UCI: %s) - %.2f%% chance\n", move.san, move.uci, move.percentage);
	            }
	            System.out.println();
            }
            
            MoveData selectedMove = selectMoveWeighted(moveList);
            System.out.printf("Randomly Selected Move: %s (UCI: %s) - %.2f%% chance\n", selectedMove.san, selectedMove.uci, selectedMove.percentage);
            System.out.println();
            return selectedMove.uci;

        } catch (IndexOutOfBoundsException e) {
        	System.out.println("No more positions found");
        	return "end";
    	} catch (Exception e) {
    		System.out.println("Invalid move, please use UCI notation (e.g., e2e4)");
    		return "invalid";
        }
    }
    
    static class MoveData {
        String san;
        String uci;
        double percentage;

        public MoveData(String san, String uci, double percentage) {
            this.san = san;
            this.uci = uci;
            this.percentage = percentage;
        }
    }
    
    public static MoveData selectMoveWeighted(List<MoveData> moves) {
        
        double totalWeight = 0;
        for (MoveData move : moves) {
            totalWeight += move.percentage;
        }
        
        double randomValue = new Random().nextDouble() * totalWeight;
        
        double cumulativeWeight = 0;
        for (MoveData move : moves) {
            cumulativeWeight += move.percentage;
            if (randomValue < cumulativeWeight) {
                return move;
            }
        }
        
        return moves.get(moves.size() - 1);
    }
}