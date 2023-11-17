import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultsFormatter {
    private final List<String> legitimatePlayers;
    private final List<String> illegalPlayerActions;
    private final long hostBalanceChange;

    // get all the data processed by BettingProcessor
    public ResultsFormatter(List<String> legitimatePlayers, List<String> illegalPlayerActions, long hostBalanceChange) {
        this.legitimatePlayers = legitimatePlayers;
        this.illegalPlayerActions = illegalPlayerActions;
        this.hostBalanceChange = hostBalanceChange;
    }

    public void writeResultsToFile(String fileName) {
        List<String> results = new ArrayList<>();

        if (legitimatePlayers.isEmpty()) {
            // In case the first or second part of the results are empty lists,
            // an empty line should be written into the output file for corresponding section.
            results.add("");
        } else {
            results.addAll(legitimatePlayers);
        }

        // separator of blocks
        results.add("");

        if (illegalPlayerActions.isEmpty()) {
            // In case the first or second part of the results are empty lists,
            // an empty line should be written into the output file for corresponding section.
            results.add("");
        } else {
            results.addAll(illegalPlayerActions);
        }

        // separator of blocks
        results.add("");

        // hostBalanceChange by default is 0
        // so if casino balance haven't been changed or all players are considered illegal
        // it will write 0
        results.add(String.valueOf(hostBalanceChange));

        // write results to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String line : results) {
                // read all written lines to results
                // and write them one by one
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
