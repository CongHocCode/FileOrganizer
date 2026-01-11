import java.io.File;

public interface Rule {
    //Tạo ra tiêu chuẩn rằng 1 rule phải biết được file này có phù hợp với rule không, nếu có thì nó thuộc folder nào để move file vào
    boolean check (File file);
    String getFolder();


}
