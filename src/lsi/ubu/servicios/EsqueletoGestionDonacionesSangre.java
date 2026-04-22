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
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		float cantidad = 0;
		float reservaDestino = 0;

	
		try{
			con = pool.getConnection();
			// comprobar tipo de sangre
			ps = con.prepareStatement("select count(*) from tipo_sangre where id_tipo_sangre = ?");
			ps.setInt(1, m_ID_Tipo_Sangre);
			rs = ps.executeQuery();
			rs.next();

			if (rs.getInt(1) == 0) {
				throw new GestionDonacionesSangreException(
						GestionDonacionesSangreException.TIPO_SANGRE_NO_EXISTE);
			}

			rs.close();
			ps.close();

			// comprobar hospital origen
			ps = con.prepareStatement("select count(*) from hospital where id_hospital = ?");
			ps.setInt(1, m_ID_Hospital_Origen);
			rs = ps.executeQuery();
			rs.next();

			if (rs.getInt(1) == 0) {
				throw new GestionDonacionesSangreException(
						GestionDonacionesSangreException.HOSPITAL_NO_EXISTE);
			}

			rs.close();
			ps.close();

			// comprobar hospital destino
			ps = con.prepareStatement("select count(*) from hospital where id_hospital = ?");
			ps.setInt(1, m_ID_Hospital_Destino);
			rs = ps.executeQuery();
			rs.next();

			if (rs.getInt(1) == 0) {
				throw new GestionDonacionesSangreException(
						GestionDonacionesSangreException.HOSPITAL_NO_EXISTE);
			}

			rs.close();
			ps.close();

			// buscar el traspaso
			ps = con.prepareStatement(
					"select cantidad from traspaso "
					+ "where id_tipo_sangre = ? "
					+ "and id_hospital_origen = ? "
					+ "and id_hospital_destino = ? "
					+ "and fecha_traspaso = ?");
			ps.setInt(1, m_ID_Tipo_Sangre);
			ps.setInt(2, m_ID_Hospital_Origen);
			ps.setInt(3, m_ID_Hospital_Destino);
			ps.setDate(4, new java.sql.Date(m_Fecha_Traspaso.getTime()));
			rs = ps.executeQuery();

			if (!rs.next()) {
				throw new GestionDonacionesSangreException(
						GestionDonacionesSangreException.VALOR_CANTIDAD_TRASPASO_INCORRECTO);
			}

			cantidad = rs.getFloat(1);

			rs.close();
			ps.close();

			// mirar reserva del hospital destino
			ps = con.prepareStatement(
					"select cantidad from reserva_hospital "
					+ "where id_tipo_sangre = ? and id_hospital = ?");
			ps.setInt(1, m_ID_Tipo_Sangre);
			ps.setInt(2, m_ID_Hospital_Destino);
			rs = ps.executeQuery();

			if (!rs.next()) {
				throw new GestionDonacionesSangreException(
						GestionDonacionesSangreException.VALOR_RESERVA_INCORRECTO);
			}

			reservaDestino = rs.getFloat(1);

			rs.close();
			ps.close();

			// comprobar que no quede negativa
			if (reservaDestino < cantidad) {
				throw new GestionDonacionesSangreException(
						GestionDonacionesSangreException.VALOR_RESERVA_INCORRECTO);
			}

			// borrar traspaso
			ps = con.prepareStatement(
					"delete from traspaso "
					+ "where id_tipo_sangre = ? "
					+ "and id_hospital_origen = ? "
					+ "and id_hospital_destino = ? "
					+ "and fecha_traspaso = ?");
			ps.setInt(1, m_ID_Tipo_Sangre);
			ps.setInt(2, m_ID_Hospital_Origen);
			ps.setInt(3, m_ID_Hospital_Destino);
			ps.setDate(4, new java.sql.Date(m_Fecha_Traspaso.getTime()));
			ps.executeUpdate();

			ps.close();

			// sumar al origen
			ps = con.prepareStatement(
					"update reserva_hospital "
					+ "set cantidad = cantidad + ? "
					+ "where id_tipo_sangre = ? and id_hospital = ?");
			ps.setFloat(1, cantidad);
			ps.setInt(2, m_ID_Tipo_Sangre);
			ps.setInt(3, m_ID_Hospital_Origen);
			ps.executeUpdate();

			ps.close();

			// restar al destino
			ps = con.prepareStatement(
					"update reserva_hospital "
					+ "set cantidad = cantidad - ? "
					+ "where id_tipo_sangre = ? and id_hospital = ?");
			ps.setFloat(1, cantidad);
			ps.setInt(2, m_ID_Tipo_Sangre);
			ps.setInt(3, m_ID_Hospital_Destino);
			ps.executeUpdate();

			ps.close();

			con.commit();
			
			
		} catch (SQLException e) {	
			if (con != null) {
				con.rollback();
			}
			
			logger.error(e.getMessage());
			throw e;		

		} finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (con != null) con.close();
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
		// TEST 1: caso correcto
		try {
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();

			anular_traspaso(1, 1, 2, java.sql.Date.valueOf("2025-01-11"));
			System.out.println("TEST 1 OK");

		} catch (SQLException e) {
			System.out.println("TEST 1 MAL");

		} finally {
			if (cll_reinicia != null) cll_reinicia.close();
			if (conn != null) conn.close();
			cll_reinicia = null;
			conn = null;
		}

		// TEST 2: tipo de sangre inexistente
		try {
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();

			anular_traspaso(99, 1, 2, java.sql.Date.valueOf("2025-01-11"));
			System.out.println("TEST 2 MAL");

		} catch (SQLException e) {
			System.out.println("TEST 2 OK");

		} finally {
			if (cll_reinicia != null) cll_reinicia.close();
			if (conn != null) conn.close();
			cll_reinicia = null;
			conn = null;
		}

		// TEST 3: hospital inexistente
		try {
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();

			anular_traspaso(1, 99, 2, java.sql.Date.valueOf("2025-01-11"));
			System.out.println("TEST 3 MAL");

		} catch (SQLException e) {
			System.out.println("TEST 3 OK");

		} finally {
			if (cll_reinicia != null) cll_reinicia.close();
			if (conn != null) conn.close();
			cll_reinicia = null;
			conn = null;
		}

		// TEST 4: traspaso inexistente
		try {
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();

			anular_traspaso(1, 1, 2, java.sql.Date.valueOf("2025-01-20"));
			System.out.println("TEST 4 MAL");

		} catch (SQLException e) {
			System.out.println("TEST 4 OK");

		} finally {
			if (cll_reinicia != null) cll_reinicia.close();
			if (conn != null) conn.close();
			cll_reinicia = null;
			conn = null;
		}

		// TEST 5: reserva insuficiente
		try {
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();

			anular_traspaso(2, 3, 2, java.sql.Date.valueOf("2025-01-16"));
			System.out.println("TEST 5 MAL");

		} catch (SQLException e) {
			System.out.println("TEST 5 OK");

		} finally {
			if (cll_reinicia != null) cll_reinicia.close();
			if (conn != null) conn.close();
			cll_reinicia = null;
			conn = null;
		}
		
	}
}
