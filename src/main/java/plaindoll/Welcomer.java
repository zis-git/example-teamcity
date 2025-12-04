package plaindoll;

public class Welcomer {

    public String sayWelcome() {
        return "Welcome home, good hunter. What is it your desire?";
    }

    public String sayFarewell() {
        return "Farewell, good hunter. May you find your worth in waking world.";
    }

    public String sayNeedGold() {
        return "Not enough gold";
    }

    public String saySome() {
        return "something in the way";
    }

    // Новый метод по заданию
    public String hunter(String name) {
        return "Hunting for " + name + "!";
    }
}

