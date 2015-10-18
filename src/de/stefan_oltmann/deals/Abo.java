package de.stefan_oltmann.deals;

import java.util.List;

public class Abo {

    private String email;
    private List<String> tags;

    public Abo(String email, List<String> tags) {
        this.email = email;
        this.tags = tags;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getTags() {
        return tags;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Abo other = (Abo) obj;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Abo [email=" + email + ", tags=" + tags + "]";
    }

}
