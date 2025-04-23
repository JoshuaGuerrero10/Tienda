/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tienda.service.Impl;

import com.tienda.service.ReporteService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRXlsxExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Servicio de generación de reportes
 * @author joshu
 */
@Service
public class ReporteServiceImpl implements ReporteService {

    @Autowired
    private DataSource datasource;

    @Override
    public ResponseEntity<Resource> generaResporte(String reporte, Map<String, Object> params, String tipo) {
        try {
            String estilo = "attachment";
            if ("vPdf".equals(tipo)) {
                estilo = "inline";
            }

            // El nombre del folder donde están los archivos .jasper
            String reportePath = "reportes";

            // Objeto donde queda el reporte generado en memoria
            ByteArrayOutputStream salida = new ByteArrayOutputStream();

            // Se define el objeto desde donde se lee el archivo .jasper
            ClassPathResource fuente = new ClassPathResource(reportePath + File.separator + reporte + ".jasper");

            // El objeto donde efectivamente se lee la información
            InputStream elReporte = fuente.getInputStream();

            // El objeto donde se crea el reporte ya generado
            JasperPrint reporteJasper = JasperFillManager.fillReport(elReporte, params, datasource.getConnection());

            // Variables para generar la respuesta
            MediaType mediaType = null;
            String archivoSalida = "";
            byte[] data;

            switch (tipo) {
                case "Pdf", "vPdf" -> {
                    JasperExportManager.exportReportToPdfStream(reporteJasper, salida);
                    mediaType = MediaType.APPLICATION_PDF;
                    archivoSalida = reporte + ".pdf";
                }
                case "Xls" -> {
                    JRXlsxExporter exportador = new JRXlsxExporter();
                    exportador.setExporterInput(new SimpleExporterInput(reporteJasper));
                    exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
                    SimpleXlsxReportConfiguration configuracion = new SimpleXlsxReportConfiguration();
                    configuracion.setDetectCellType(true);
                    configuracion.setCollapseRowSpan(true);
                    exportador.setConfiguration(configuracion);
                    exportador.exportReport();
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    archivoSalida = reporte + ".xlsx";
                }
                case "Csv" -> {
                    JRCsvExporter exportador = new JRCsvExporter();
                    exportador.setExporterInput(new SimpleExporterInput(reporteJasper));
                    exportador.setExporterOutput(new SimpleWriterExporterOutput(salida));
                    exportador.exportReport();
                    mediaType = MediaType.TEXT_PLAIN;
                    archivoSalida = reporte + ".csv";
                }
                default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + tipo);
            }

            data = salida.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Disposition", estilo + "; filename=\"" + archivoSalida + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(data.length)
                    .contentType(mediaType)
                    .body(new InputStreamResource(new ByteArrayInputStream(data)));

        } catch (SQLException | JRException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}