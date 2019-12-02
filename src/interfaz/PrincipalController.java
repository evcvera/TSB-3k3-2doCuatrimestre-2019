package interfaz;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import negocio.Agrupaciones;
import negocio.Region;
import negocio.Regiones;
import negocio.Resultados;

import java.io.File;
import java.io.FileNotFoundException;

public class PrincipalController {
    public Button btnCargar;
    public Button btnCambiar;
    public Label lblCarpeta;
    public ListView lvwResultados;
    public ComboBox cboDistritos;
    public ComboBox cboSecciones;
    public ComboBox cboCircuito;
    public ComboBox cboMesas;
    public Button btnMostrarPais;
    private Resultados resultados;

    private Regiones regiones;
    private final Alert loadingDialog = new Alert(Alert.AlertType.INFORMATION);

    public void cargar(ActionEvent actionEvent) {
        loadingDialog.setHeaderText("Cargando Datos, por favor espere...");
        loadingDialog.show();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Runnable updater = new Runnable() {

                    @Override
                    public void run() {
                        String carpeta = lblCarpeta.getText();
                        try{
                            Agrupaciones agrupaciones = new Agrupaciones(carpeta);

                            regiones = new Regiones(carpeta);
                            resultados = new Resultados(agrupaciones, regiones.getPais(), carpeta);
                            mostrarResultadosRegion(regiones.getPais().getCodigo());
                            cboDistritos.setItems(FXCollections.observableArrayList(regiones.getPais().getSubregiones()));

                            btnMostrarPais.setDisable(false);
                            loadingDialog.close();
                        } catch (FileNotFoundException e){
                            loadingDialog.close();
                            Alert errorDialog = new Alert(Alert.AlertType.ERROR);
                            errorDialog.setHeaderText("Error al cargar resultados, por favor verifique la carpeta seleccionada");
                            errorDialog.show();
                        }

                    }
                };
                Platform.runLater(updater);

            }

        });

        thread.setDaemon(true);
        thread.start();
    }

    public void cambiarUbicacion(ActionEvent actionEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        File file = dc.showDialog(null);
        if (file != null) {
            lblCarpeta.setText(file.getPath());
        }
    }

    private void mostrarResultadosRegion(String codRegion) {
        lvwResultados.getItems().clear();
        ObservableList ol = FXCollections.observableArrayList(resultados.getResultados(codRegion));
        lvwResultados.setItems(ol);
    }

    public void elegirPais(ActionEvent actionEvent) {
        cboDistritos.setValue(null);
        mostrarResultadosRegion(regiones.getPais().getCodigo());
    }

    public void elegirDistrito(ActionEvent actionEvent) {
        Region r = (Region) cboDistritos.getValue();
        if (r != null) {
            cboSecciones.setItems(FXCollections.observableArrayList(r.getSubregiones()));
            mostrarResultadosRegion(r.getCodigo());
        } else
            cboSecciones.setItems(null);
    }

    public void elegirSeccion(ActionEvent actionEvent) {
        Region r = (Region) cboSecciones.getValue();
        if (r != null) {
            cboCircuito.setItems(FXCollections.observableArrayList(r.getSubregiones()));
            mostrarResultadosRegion(r.getCodigo());
        } else
            cboCircuito.setItems(null);
    }

    public void elegirCircuito(ActionEvent actionEvent) {
        Region r = (Region) cboCircuito.getValue();
        if (r != null) {
            cboMesas.setItems(FXCollections.observableArrayList(r.getSubregiones()));
            mostrarResultadosRegion(r.getCodigo());
        }
        else
            cboMesas.setItems(null);
    }


    public void elegirMesa(ActionEvent actionEvent) {
            Region r = (Region) cboMesas.getValue();
            if (r != null) {
                mostrarResultadosRegion(r.getCodigo());
            }
    }

}
