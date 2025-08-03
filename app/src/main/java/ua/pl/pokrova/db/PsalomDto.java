package ua.pl.pokrova.db;

public class PsalomDto {
    int id, id_kaf;
    String name, short_desc, desc;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_kaf() {
        return id_kaf;
    }

    public void setId_kaf(int id_kaf) {
        this.id_kaf = id_kaf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShort_desc() {
        return short_desc;
    }

    public void setShort_desc(String short_desc) {
        this.short_desc = short_desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
