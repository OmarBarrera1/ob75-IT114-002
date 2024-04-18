package Project.Common;

//UCID - ob75 - April 10, 2024

public class PrivateMPayload extends Payload{
    private String clientName;

    public PrivateMPayload(String clientName2, String message2){
        this.clientName = clientName2;
        setMessage(message2);
        setPayloadType(PayloadType.PM);
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }


}
