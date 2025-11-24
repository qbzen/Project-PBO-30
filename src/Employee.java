import java.sql.Connection;

public abstract class Employee {
    protected int id;
    protected String name;
    protected String employmentType;

    public Employee(int id, String name, String employmentType) {
        this.id = id;
        this.name = name;
        this.employmentType = employmentType;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmploymentType() { return employmentType; }

    public abstract double calculatePay(int year, int month, Connection conn) throws Exception;
}
