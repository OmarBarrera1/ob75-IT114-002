package Project.Common;

//UCID - ob75 - April 13, 2024

public class Mute_UnmutePayload extends Payload{
    private String clientMuteName;
    private String clientUnmuteName;
    
    public void MutePayload(String clientMuteName2){
        this.clientMuteName = clientMuteName2;
        setPayloadType(PayloadType.MUTE);
    }

    public void UnmutePayload(String clientUnmuteName2){
        this.clientUnmuteName = clientUnmuteName2;
        setPayloadType(PayloadType.UNMUTE);
    }

    public String getClientMuteName() {
        return clientMuteName;
    }

    public void setClientMuteName(String clientMuteName) {
        this.clientMuteName = clientMuteName;
    }

    public String getClientUnmuteName() {
        return clientUnmuteName;
    }

    public void setClientUnmuteName(String clientUnmuteName) {
        this.clientUnmuteName = clientUnmuteName;
    }
    
}
