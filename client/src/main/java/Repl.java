import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static ui.EscapeSequences.*;
public class Repl {

    public void run() {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.println("Intro");
        out.print("Options");

        Scanner scanner = new Scanner(System.in);
        var result = "";
        while (!result.equals("quit")) {
            printPrompt(out);
            String line = scanner.nextLine();

            try {
                result = ChessClient.evaluate(line);
            } catch (Throwable e) {
                out.print(e);
            }
        }
        out.println();
    }

    private void printPrompt(PrintStream out) {
        out.print("\n" + UNSET_TEXT_COLOR + ">>> " + SET_TEXT_COLOR_GREEN);
    }
}