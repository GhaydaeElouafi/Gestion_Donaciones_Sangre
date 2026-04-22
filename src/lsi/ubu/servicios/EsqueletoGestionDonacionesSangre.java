package lsi.ubu.servicios;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
		consulta_traspasos("Tipo A.");

		System.out.println("FIN.............");
	}
	
	public static void realizar_donacion(String m_NIF, int m_ID_Hospital,
			float m_Cantidad,  Date m_Fecha_Donacion) throws SQLException {
		
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
		PreparedStatement st = null;
		ResultSet rs = null;
		PreparedStatement stCheck = null;
		ResultSet rsCheck = null;
	
		try{
			con = pool.getConnection();
			
			String check = "SELECT id_tipo_sangre FROM tipo_sangre WHERE descripcion = ?";
	        stCheck = con.prepareStatement(check);
	        stCheck.setString(1, m_Tipo_Sangre);

	        rsCheck = stCheck.executeQuery();

	        if (!rsCheck.next()) {
	            throw new GestionDonacionesSangreException(
	                GestionDonacionesSangreException.TIPO_SANGRE_NO_EXISTE
	            );
	        }
	       
			String sql = "SELECT t.id_traspaso, t.cantidad, t.fecha_traspaso, " +
                    "h.nombre, ts.descripcion " +
                    "FROM traspaso t " +
                    "JOIN hospital h ON t.id_hospital_destino = h.id_hospital " +
                    "JOIN tipo_sangre ts ON t.id_tipo_sangre = ts.id_tipo_sangre " +
                    "WHERE ts.descripcion = ? " +
                    "ORDER BY t.id_hospital_destino, t.fecha_traspaso";
			
			st = con.prepareStatement(sql);
	        st.setString(1, m_Tipo_Sangre);

	        rs = st.executeQuery();

	        while (rs.next()) {
	            System.out.println(
	                rs.getInt("id_traspaso") + " - " +
	                rs.getString("nombre") + " - " +
	                rs.getString("descripcion") + " - " +
	                rs.getFloat("cantidad") + " - " +
	                rs.getDate("fecha_traspaso")
	            );
	        }

	        con.commit();
			
		} catch (SQLException e) {
			//Completar por el alumno (YA)			
			if (con != null) con.rollback();
			logger.error(e.getMessage());
			throw e;		

		} finally {
			/*A rellenar por el alumno (YA)*/
			if (rs != null) rs.close();
	        if (st != null) st.close();
	        if (con != null) con.close();
	        if (stCheck != null) stCheck.close();
	        if (rsCheck != null) rsCheck.close();
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
