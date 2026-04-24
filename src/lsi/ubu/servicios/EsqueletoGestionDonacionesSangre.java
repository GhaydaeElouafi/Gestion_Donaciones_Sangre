package lsi.ubu.servicios;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.util.ExecuteScript;
import lsi.ubu.util.PoolDeConexiones;


/**
 * GestionDonacionesSangre:
 * Implementa la gestion de donaciones de sangre según el enunciado del ejercicio
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jesus Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Raul Marticorena</a>
 * @author <a href="mailto:pgdiaz@ubu.es">Pablo Garcia</a>
 * @author <a href="mailto:srarribas@ubu.es">Sandra Rodriguez</a>
 * @version 1.5
 * @since 1.0 
 */
public class EsqueletoGestionDonacionesSangre {
	
	private static Logger logger = LoggerFactory.getLogger(EsqueletoGestionDonacionesSangre.class);

	private static final String script_path = "sql/";

	public static void main(String[] args) throws SQLException{		
		tests();

		System.out.println("FIN.............");
	}
	
	public static void realizar_donacion(String m_NIF, int m_ID_Hospital,
			float m_Cantidad,  Date m_Fecha_Donacion) throws SQLException {
		
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		Connection con=null;
	    java.sql.PreparedStatement ps = null;
	    java.sql.ResultSet rs = null;

	    try {
	        con = pool.getConnection();
	        con.setAutoCommit(false);

	        // Comprobamos donante
	        ps = con.prepareStatement("SELECT 1 FROM donante WHERE nif = ?");
	        ps.setString(1, m_NIF);
	        rs = ps.executeQuery();

	        if (!rs.next()) {
	            throw new GestionDonacionesSangreException(GestionDonacionesSangreException.DONANTE_NO_EXISTE);
	        }
	        rs.close();
	        ps.close();

	        // Comprobamos el hospital
	        ps = con.prepareStatement("SELECT 1 FROM hospital WHERE id_hospital = ?");
	        ps.setInt(1, m_ID_Hospital);
	        rs = ps.executeQuery();

	        if (!rs.next()) {
	            throw new GestionDonacionesSangreException(GestionDonacionesSangreException.HOSPITAL_NO_EXISTE);
	        }
	        rs.close();
	        ps.close();

	        // Validamos al cantidad
	        if (m_Cantidad <= 0 || m_Cantidad > 0.45f) {
	            throw new GestionDonacionesSangreException(GestionDonacionesSangreException.VALOR_CANTIDAD_DONACION_INCORRECTO);
	        }

	        // Miramos que la ultima donacion no haya sido hace menos de 15 dias
	        ps = con.prepareStatement(
	            "SELECT MAX(fecha_donacion) FROM donacion WHERE nif_donante = ?");
	        ps.setString(1, m_NIF);
	        rs = ps.executeQuery();

	        if (rs.next() && rs.getDate(1) != null) {

	            long diff = m_Fecha_Donacion.getTime() - rs.getDate(1).getTime();
	            long dias = diff / (1000L * 60 * 60 * 24);

	            if (dias < 15) {
	                throw new GestionDonacionesSangreException(GestionDonacionesSangreException.DONANTE_EXCEDE);
	            }
	        }

	        rs.close();
	        ps.close();

	        // Insertamos la donacion
	        ps = con.prepareStatement(
	            "INSERT INTO donacion (nif_donante, cantidad, fecha_donacion) VALUES (?, ?, ?)");
	        ps.setString(1, m_NIF);
	        ps.setFloat(2, m_Cantidad);
	        ps.setDate(3, new java.sql.Date(m_Fecha_Donacion.getTime()));
	        ps.executeUpdate();
	        ps.close();

	        // Obtenemos el tipo de sangre
	        ps = con.prepareStatement(
	            "SELECT id_tipo_sangre FROM donante WHERE nif = ?");
	        ps.setString(1, m_NIF);
	        rs = ps.executeQuery();
	        rs.next();

	        int tipo = rs.getInt(1);

	        rs.close();
	        ps.close();

	        // Actualizamos la reserva del hospital
	        ps = con.prepareStatement(
	            "UPDATE reserva_hospital SET cantidad = cantidad + ? " +
	            "WHERE id_hospital = ? AND id_tipo_sangre = ?");

	        ps.setFloat(1, m_Cantidad);
	        ps.setInt(2, m_ID_Hospital);
	        ps.setInt(3, tipo);
	        ps.executeUpdate();

	        con.commit();

	    } catch (SQLException e) {

	        if (con != null) {
	            con.rollback(); } 

	        logger.error(e.getMessage());
	        throw e;

	    } finally {

	        try { if (rs != null) rs.close(); } catch (SQLException e) {}
	        try { if (ps != null) ps.close(); } catch (SQLException e) {}
	        try { if (con != null) con.close(); } catch (SQLException e) {}
	    }
		
		
		
	}
	
	public static void anular_traspaso(int m_ID_Tipo_Sangre, int m_ID_Hospital_Origen,int m_ID_Hospital_Destino,
			Date m_Fecha_Traspaso)
			throws SQLException {
		
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		Connection con=null;

	
		try{
			con = pool.getConnection();
			//Completar por el alumno
			
		} catch (SQLException e) {
			//Completar por el alumno			
			
			logger.error(e.getMessage());
			throw e;		

		} finally {
			/*A rellenar por el alumno*/
		}		
	}
	
	public static void consulta_traspasos(String m_Tipo_Sangre)
			throws SQLException {

				
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		Connection con=null;

	
		try{
			con = pool.getConnection();
			//Completar por el alumno
			
		} catch (SQLException e) {
			//Completar por el alumno			
			
			logger.error(e.getMessage());
			throw e;		

		} finally {
			/*A rellenar por el alumno*/
		}		
	}
	
	static public void creaTablas() {
		ExecuteScript.run(script_path + "gestion_donaciones_sangre.sql");
	}

	static void tests() throws SQLException{
		creaTablas();
		
		PoolDeConexiones pool = PoolDeConexiones.getInstance();		
		
		//Relatar caso por caso utilizando el siguiente procedure para inicializar los datos
		
		CallableStatement cll_reinicia=null;
		Connection conn = null;
		
		try {
			//Reinicio filas
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();
		} catch (SQLException e) {				
			logger.error(e.getMessage());			
		} finally {
			if (cll_reinicia!=null) cll_reinicia.close();
			if (conn!=null) conn.close();
		
		}			
		
	}
}
