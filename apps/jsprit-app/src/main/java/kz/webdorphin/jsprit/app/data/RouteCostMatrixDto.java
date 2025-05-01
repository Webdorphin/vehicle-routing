package kz.webdorphin.jsprit.app.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RouteCostMatrixDto {

    private double[][] matrix;
}
