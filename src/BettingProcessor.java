import java.util.*;
import java.util.stream.Collectors;

public class BettingProcessor {

    // playerBalances are needed at the end to calculate casino balance
    // and to calculate legitimatePlayers which goes to first section of results
    Map<String, Long> playerBalances = new HashMap<>();

    // playerDeposits are needed at the end to calculate casino balance
    // playerDeposits is only sum of deposits
    // bets do not impact playerDeposits
    Map<String, Integer> playerDeposits = new HashMap<>();

    // playerWins are needed to calculate player winrate
    Map<String, Integer> playerWins = new HashMap<>();

    // playerBets are needed to calculate player winrate
    Map<String, Integer> playerBets = new HashMap<>();

    // illegalPlayers can be extracted from illegalActions
    // but for convenience i made it separate variable
    Set<String> illegalPlayers = new HashSet<>();

    // illegalActions is a set of strings that are considered illegal
    // needed to calculate illegalPlayerActions
    List<String> illegalActions = new ArrayList<>();

    // illegalPlayerActions is needed to write 2nd section to result
    // it's almost the same as illegalActions
    List<String> illegalPlayerActions = new ArrayList<>();

    // illegalPlayerActions is needed to write 1st section to result
    List<String> legitimatePlayers = new ArrayList<>();

    // hostBalanceChange is long according to those 2 strings
    // Coin numbers in transactions are int type, account balance values are long.
    // Coin changes in casino host balance
    long hostBalanceChange = 0;
    public void processBettingData(List<String> playerData, List<String> matchData) {

        // split string on blocks to get opations/uuid/...
        for (String line : playerData) {
            String[] values = line.split(",");
            String playerId = values[0];
            String operation = values[1];

            // process operations
            switch (operation) {
                case "DEPOSIT" -> {
                    long depositAmount = Long.parseLong(values[3]);
                    playerBalances.put(playerId, playerBalances.getOrDefault(playerId, 0L) + depositAmount);
                    playerDeposits.put(playerId, (playerDeposits.getOrDefault(playerId, 0) +  (int) depositAmount));
                }
                case "BET" -> {
                    String matchId = values[2];
                    int betAmount = Integer.parseInt(values[3]);
                    String side = values[4];
                    if (betAmount > playerBalances.get(playerId)) {
                        illegalPlayers.add(playerId);
                        illegalActions.add(line);
                    } else if (side.equals("A") || side.equals("B")){
                        // Process the bet
                        String result = getResult(matchData, matchId);
                        if (result.equals(side)) {
                            // Player won the bet
                            double rate = getRate(matchData, matchId, side);
                            int winnings = (int) (betAmount * rate);
                            playerBalances.put(playerId, playerBalances.get(playerId) + winnings);
                            playerWins.put(playerId, playerWins.getOrDefault(playerId, 0) + 1);
                        } else if (result.equals("DRAW")) {
                            // Bet is a draw - no action needed
                            // count draw as bet too
                            playerBets.put(playerId, playerBets.getOrDefault(playerId, 0) + 1);
                            continue;
                        } else {
                            // player lost the bet - lost the coins
                            playerBalances.put(playerId, playerBalances.get(playerId) - betAmount);
                        }
                        playerBets.put(playerId, playerBets.getOrDefault(playerId, 0) + 1);

                    }
                    else // in case there's winning side "AB"/"C" or anything else
                        throw new IllegalArgumentException("Invalid side value: " + side);
                }
                case "WITHDRAW" -> {
                    long withdrawAmount = Long.parseLong(values[3]);
                    if (withdrawAmount > playerBalances.get(playerId))
                    {
                        // user is trying to withdraw more than he has on balance - illegal
                        illegalActions.add(line);
                        illegalPlayers.add(playerId);

                    } else {
                        // legal withdrawal - process
                        playerBalances.put(playerId, playerBalances.get(playerId) - withdrawAmount);
                        playerDeposits.put(playerId, (playerDeposits.getOrDefault(playerId, 0) -  (int) withdrawAmount));
                    }
                }
            }
        }

        // calculate 1st block of legitimate players
        legitimatePlayers = playerBalances.keySet().stream()
                .filter(playerId -> illegalPlayers.stream().noneMatch(action -> action.startsWith(playerId)))
                .sorted()
                .map(playerId -> {
                    double winRate = playerBets.containsKey(playerId) ? (double) playerWins.getOrDefault(playerId, 0) / playerBets.get(playerId) : 0.0;
                    return playerId + " " + playerBalances.get(playerId) + " " + String.format("%.2f", winRate);
                })
                .toList();

        // calculate 2nd block of illegal players
        illegalPlayerActions = illegalActions.stream()
                .filter(line -> illegalPlayers.contains(line.split(",")[0]))
                .collect(Collectors.toMap(
                        line -> line.split(",")[0],  // key: playerId
                        line -> {
                            String[] values = line.split(",");
                            String playerId = values[0];
                            String operation = values[1];

                            return switch (operation) {
                                case "BET" -> playerId + " BET " + values[2] + " " + values[3] + " " + values[4];
                                case "WITHDRAW" -> playerId + " WITHDRAW null " + values[3] + " null";
                                default -> "";
                            };
                        },
                        (existing, replacement) -> existing // If a playerId already exists, keep the existing value
                        // so there will be only first illegal action
                ))
                .values()
                .stream()
                .sorted()
                .toList();

    }

    public long getHostBalanceChange() {

        // create a new map to store valid player balances
        // this is needed to calculate casino balance by formula = playerDeposits - player(end)Balance
        Map<String, Long> validPlayerBalances = new HashMap<>();

        // Iterate over balances to filter out illegal players
        for (Map.Entry<String, Long> balancesEntry : playerBalances.entrySet()) {
            String playerName = balancesEntry.getKey();

            // Check if the player is not in the list of illegal players
            if (illegalPlayers == null || !illegalPlayers.contains(playerName))
            {
                // if player is valid - he is added to validPlayerBalances obviously
                validPlayerBalances.put(playerName, balancesEntry.getValue());
            }
        }

        // calculate hostBalanceChange based on the valid entries

        // validPlayerBalances contains of valid user balances <uuid, balance>
        // no need to calculate validPlayerDeposits as we can just take uuid of validPlayerBalances and
        // find deposit value of player with same uuid
        for (Map.Entry<String, Long> balancesEntry : validPlayerBalances.entrySet()) {
            String playerName = balancesEntry.getKey();
            long endBalance = balancesEntry.getValue();

            // make sure the player is in the deposits map
            if (playerDeposits.containsKey(playerName)) {
                long deposits = playerDeposits.get(playerName);
                hostBalanceChange += deposits - endBalance;
            }
        }
        return hostBalanceChange;
    }

    // return string to be proccesed by ResultsFormatter
    public List<String> getLegitimatePlayers() {
        return legitimatePlayers;
    }

    // return string to be proccesed by ResultsFormatter
    public List<String> getIllegalPlayerActions() {
        return illegalPlayerActions;
    }

    // get result of match via splitting the line
    private static String getResult(List<String> matchData, String matchId) {
        for (String line : matchData) {
            String[] values = line.split(",");
            if (values[0].equals(matchId)) {
                String result = values[3];
                if (!result.equals("A") && !result.equals("B") && !result.equals("DRAW")) {
                    throw new IllegalArgumentException("Invalid result value: " + result);
                }
                return result;
            }
        }
        throw new IllegalArgumentException("No matching result found for matchId: " + matchId);
    }

    // get match rate  via splitting the line
    // if side is equal to "A", then return the double value parsed from values[1] (which represents the rate for side "A").
    //if side is not equal to "A" (i.e., it's assumed to be "B" in this case), then return the double value parsed from values[2] (which represents the rate for side "B").
    private static double getRate(List<String> matchData, String matchId, String side) {
        for (String line : matchData) {
            String[] values = line.split(",");
            if (values[0].equals(matchId)) {
                return side.equals("A") ? Double.parseDouble(values[1]) : Double.parseDouble(values[2]);
            }
        }
        throw new IllegalArgumentException("No matching rate found for matchId: " + matchId);
    }

}