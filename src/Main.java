import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        BettingProcessor bettingProcessor = new BettingProcessor();

        // read input files
        List<String> playerData = readFile("res/player_data.txt");
        List<String> matchData = readFile("res/match_data.txt");

        // process data in BettingProcessor class
        bettingProcessor.processBettingData(playerData, matchData);

        // call ResultsFormatter and write results to output file
        ResultsFormatter resultsFormatter = new ResultsFormatter(
                bettingProcessor.getLegitimatePlayers(),
                bettingProcessor.getIllegalPlayerActions(),
                bettingProcessor.getHostBalanceChange()
        );
        resultsFormatter.writeResultsToFile("src/result.txt");

    }

    // read all the files
    private static List<String> readFile(String fileName) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            lines = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}