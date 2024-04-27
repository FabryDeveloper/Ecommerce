package it.polimi.tiw.ecommerceCS.beans;

import java.util.Objects;

public class Product {
	private int id;
	private String name;
	private String description;
	private String category;
	private String photo;

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
	
	public String getCategory() {
		return category;
	}
	
	public String getPhoto() {
		return photo;
	}
	
	public void setId(int i) {
		id = i;
	}

	public void setName(String n) {
		name = n;
	}

	public void setDescription(String d) {
		description = d;
	}
	
	public void setCategory(String c) {
		category = c;
	}
	
	public void setPhoto(String p) {
		photo = p;
	}
	
	// Necessario per verificare l'uguaglianza tra prodotti nelle mappe tramite i metodi predefiniti
	@Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Product other = (Product) obj;
        return id == other.id;
    }
}
