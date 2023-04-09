package suzumiya.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppDescription {
    private String iconUrl; // 程序图标
    private String appName; // 程序名称
    private String appUrl; // 程序url
    private Boolean isAvailable; // 是否可用
}
