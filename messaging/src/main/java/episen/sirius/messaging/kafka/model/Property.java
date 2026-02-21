package episen.sirius.messaging.kafka.model;

public class Property {

    private String address;

    public Property() {
    }

    public Property(String address) {
        this.address = address;
    }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }
}
