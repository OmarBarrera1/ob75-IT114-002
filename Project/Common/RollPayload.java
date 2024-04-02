package Project.Common;

//UCID - ob75 - March 27, 2024

public class RollPayload extends Payload{
    private int sides;
    private int dice;
    public RollPayload(int dice2, int sides2){
        this.dice = dice2;
        this.sides = sides2;
        setPayloadType(PayloadType.ROLL);
       
    }
    public int getSides() {
        return sides;
    }
    public void setSides(int sides) {
        this.sides = sides;
    }
    public int getDice() {
        return dice;
    }
    public void setDice(int dice) {
        this.dice = dice;
    }
}