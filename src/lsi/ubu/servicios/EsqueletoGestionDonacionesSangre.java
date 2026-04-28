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
 * @author <a href="mailto:sax1044@alu.ubu.es">Shalom Abraham</a>
 * @author <a href="mailto:gex1005@alu.ubu.es">Ghaydae Elouafi</a>
 * @author <a href="mailto:npx1005@alu.ubu.es">Narcisa Pavel</a>
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
	            "INSERT INTO donacion (id_donacion, nif_donante, cantidad, fecha_donacion) VALUES (seq_donacion.NEXTVAL, ?, ?, ?)");
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
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		float cantidad = 0;
		float reservaDestino = 0;

	
		try{
			con = pool.getConnection();
			con.setAutoCommit(false);
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
		PreparedStatement st = null;
		ResultSet rs = null;
	
		try{
			con = pool.getConnection();
			
	       
	        String sql = "SELECT t.id_traspaso, t.cantidad, t.fecha_traspaso, " +
	                "h.nombre, ts.descripcion, r.cantidad AS reserva " +
	                "FROM traspaso t " +
	                "JOIN hospital h ON t.id_hospital_destino = h.id_hospital " +
	                "JOIN tipo_sangre ts ON t.id_tipo_sangre = ts.id_tipo_sangre " +
	                "JOIN reserva_hospital r ON r.id_tipo_sangre = t.id_tipo_sangre " +
	                "AND r.id_hospital = t.id_hospital_destino " +
	                "WHERE ts.descripcion = ? " +
	                "ORDER BY t.id_hospital_destino, t.fecha_traspaso";

			st = con.prepareStatement(sql);
	        st.setString(1, m_Tipo_Sangre);

	        rs = st.executeQuery();
	        
	        boolean hayResultados = false;

	        while (rs.next()) {
	            hayResultados = true;

	            System.out.println(
	                rs.getInt("id_traspaso") + " - " +
	                rs.getString("nombre") + " - " +
	                rs.getString("descripcion") + " - " +
	                rs.getFloat("cantidad") + " - " +
	                rs.getDate("fecha_traspaso") + " - " +
	                rs.getFloat("reserva")
	            );
	        }

	        if (!hayResultados) {
	            throw new GestionDonacionesSangreException(
	                GestionDonacionesSangreException.TIPO_SANGRE_NO_EXISTE
	            );
	        }
			
		} catch (SQLException e) {			
			if (con != null) con.rollback();
			logger.error(e.getMessage());
			throw e;		

		} finally {
			if (rs != null) rs.close();
	        if (st != null) st.close();
	        if (con != null) con.close();
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
		
    // Tests del metodo consulta_traspasos() :
		// TEST CONSULTA 1: caso correcto
		System.out.println("\nTEST METODO CONSULTA_TRAPASOS\n");
		PreparedStatement psCheck = null;
		ResultSet rsCheck = null;
		try {
			conn = pool.getConnection();

		    cll_reinicia = conn.prepareCall("{call inicializa_test}");
		    cll_reinicia.execute();

		    System.out.println("Salida obtenida:");
		    consulta_traspasos("Tipo A.");

		    // comprobar que hay datos en BD 
		    psCheck = conn.prepareStatement(
		        "SELECT COUNT(*) FROM traspaso t " +
		        "JOIN tipo_sangre ts ON t.id_tipo_sangre = ts.id_tipo_sangre " +
		        "WHERE ts.descripcion = ?"
		    );

		    psCheck.setString(1, "Tipo A.");
		    rsCheck = psCheck.executeQuery();
		    rsCheck.next();

		    if (rsCheck.getInt(1) > 0) {
		        System.out.println("TEST CONSULTA 1 OK\n");
		    } else {
		        System.out.println("TEST CONSULTA 1 MAL");
		    }

		} catch (SQLException e) {
			logger.error(e.getMessage());
		    System.out.println("TEST CONSULTA 1 MAL");

		} finally {
		    if (cll_reinicia != null) cll_reinicia.close();
		    if (conn != null) conn.close();
		    cll_reinicia = null;
		    conn = null;
		}
		
		// TEST CONSULTA 2: tipo inexistente
		try {
		    conn = pool.getConnection();
		    cll_reinicia = conn.prepareCall("{call inicializa_test}");
		    cll_reinicia.execute();

		    consulta_traspasos("XYZ");
		    System.out.println("TEST CONSULTA 2 MAL");

		} catch (SQLException e) {
			logger.error(e.getMessage());
		    System.out.println("TEST CONSULTA 2 OK");
		}finally {
		    if (cll_reinicia != null) cll_reinicia.close();
		    if (conn != null) conn.close();
		    cll_reinicia = null;
		    conn = null;
		}

    // Tests del metodo anular_traspaso() :
		// TEST 1: caso correcto
		
		System.out.println("\nTEST METODO ANULAR_TRASPASO\n");
		PreparedStatement ps = null;
		ResultSet rs = null;
		float cantidad = 0;
		try {
			conn = pool.getConnection();
			cll_reinicia = conn.prepareCall("{call inicializa_test}");
			cll_reinicia.execute();

			anular_traspaso(1, 1, 2, java.sql.Date.valueOf("2025-01-11"));
			
			// comprobar que el traspaso ya no existe
		    ps = conn.prepareStatement(
		        "select count(*) from traspaso " +
		        "where id_tipo_sangre = ? and id_hospital_origen = ? " +
		        "and id_hospital_destino = ? and fecha_traspaso = ?");

		    ps.setInt(1, 1);
		    ps.setInt(2, 1);
		    ps.setInt(3, 2);
		    ps.setDate(4, java.sql.Date.valueOf("2025-01-11"));

		    rs = ps.executeQuery();
		    rs.next();

		    if (rs.getInt(1) == 0) {
		        System.out.println("TEST 1 OK: traspaso borrado");
		    } else {
		        System.out.println("TEST 1 MAL: traspaso no borrado");
		    }
		    
		    rs.close();
		    ps.close();

		    // comprobar reserva origen
		    ps = conn.prepareStatement(
		        "select cantidad from reserva_hospital where id_tipo_sangre = ? and id_hospital = ?");
		    ps.setInt(1, 1);
		    ps.setInt(2, 1);

		    rs = ps.executeQuery();
		    rs.next();
		    cantidad = rs.getFloat(1);

		    if (cantidad > 5.44 && cantidad < 5.46) {
		        System.out.println("TEST 1 OK: reserva origen correcta");
		    } else {
		        System.out.println("TEST 1 MAL: reserva origen incorrecta");
		    }

		    rs.close();
		    ps.close();
		    
		 // comprobar reserva destino
		    ps = conn.prepareStatement(
		        "select cantidad from reserva_hospital where id_tipo_sangre = ? and id_hospital = ?");
		    ps.setInt(1, 1);
		    ps.setInt(2, 2);

		    rs = ps.executeQuery();
		    rs.next();
		    cantidad = rs.getFloat(1);

		    if (cantidad > 0.44 && cantidad < 0.46) {
		        System.out.println("TEST 1 OK: reserva destino correcta");
		    } else {
		        System.out.println("TEST 1 MAL: reserva destino incorrecta");
		    }
			

		} catch (SQLException e) {
			System.out.println("TEST 1 MAL: lanzó excepción");

		} finally {
			if (rs != null) rs.close();
		    if (ps != null) ps.close();
		    if (cll_reinicia != null) cll_reinicia.close();
		    if (conn != null) conn.close();

		    rs = null;
		    ps = null;
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
			if (e.getErrorCode() == GestionDonacionesSangreException.TIPO_SANGRE_NO_EXISTE) {
				System.out.println("TEST 2 OK");
			} else {
				System.out.println("TEST 2 MAL");
			}

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
			if (e.getErrorCode() == GestionDonacionesSangreException.HOSPITAL_NO_EXISTE) {
				System.out.println("TEST 3 OK");
			} else {
				System.out.println("TEST 3 MAL");
			}

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
			if (e.getErrorCode() == GestionDonacionesSangreException.VALOR_CANTIDAD_TRASPASO_INCORRECTO) {
				System.out.println("TEST 4 OK");
			} else {
				System.out.println("TEST 4 MAL");
			}

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
			if (e.getErrorCode() == GestionDonacionesSangreException.VALOR_RESERVA_INCORRECTO) {
				System.out.println("TEST 5 OK: codigo correcto");
			} else {
				System.out.println("TEST 5 MAL: codigo incorrecto");
			}

			// comprobar que el traspaso sigue existiendo
			ps = conn.prepareStatement(
					"select count(*) from traspaso "
					+ "where id_tipo_sangre = ? and id_hospital_origen = ? "
					+ "and id_hospital_destino = ? and fecha_traspaso = ?");
			ps.setInt(1, 2);
			ps.setInt(2, 3);
			ps.setInt(3, 2);
			ps.setDate(4, java.sql.Date.valueOf("2025-01-16"));
			rs = ps.executeQuery();
			rs.next();

			if (rs.getInt(1) == 1) {
				System.out.println("TEST 5 OK: rollback mantiene traspaso");
			} else {
				System.out.println("TEST 5 MAL: rollback en traspaso");
			}

			rs.close();
			ps.close();

			// comprobar que la reserva sigue en 5.5
			ps = conn.prepareStatement(
					"select cantidad from reserva_hospital where id_tipo_sangre = ? and id_hospital = ?");
			ps.setInt(1, 2);
			ps.setInt(2, 2);
			rs = ps.executeQuery();
			rs.next();
			cantidad = rs.getFloat(1);

			if (cantidad > 5.49 && cantidad < 5.51) {
				System.out.println("TEST 5 OK: rollback mantiene reserva");
			} else {
				System.out.println("TEST 5 MAL: rollback en reserva");
			}

		} finally {
			if (cll_reinicia != null) cll_reinicia.close();
			if (conn != null) conn.close();
			cll_reinicia = null;
			conn = null;
		}

		// Tests del metodo realizar_donacion() :
		// Test 1: Todo funciona bien
				System.out.println("\nTEST METODO REALIZAR_DONACION\n");
				System.out.println("TEST DONACION 1: Todo funciona bien");
				
			    try {
			        conn = pool.getConnection();
			        cll_reinicia = conn.prepareCall("{call inicializa_test}");
			        cll_reinicia.execute();

			        realizar_donacion("12345678A", 1, 0.3f, java.sql.Date.valueOf("2026-01-01"));

			        // Comprobamos la base de datos
		                 ps = conn.prepareStatement(
		                    "SELECT COUNT(*) FROM donacion WHERE nif_donante = ? AND fecha_donacion = ?");
		                 ps.setString(1, "12345678A");
		                 ps.setDate(2, java.sql.Date.valueOf("2026-01-01"));
		                 rs = ps.executeQuery();
		                 rs.next();

		                 if (rs.getInt(1) == 1) {
		                     System.out.println("TEST DONACION 1 OK");
		                 } else {
		                     System.out.println("TEST DONACION 1 MAL (no se ha insertado la donacion)");
		                 }

		                 rs.close();
		                 ps.close();

			    } catch (SQLException e) {
			        System.out.println("TEST DONACION 1 MAL");

			    } finally {
			        if (cll_reinicia != null) cll_reinicia.close();
			        if (conn != null) conn.close();
			    }
	    
	    
	    //Test 2: El donante no existe
		System.out.println("TEST DONACION 2: El donante no existe");

	    try {
	        conn = pool.getConnection();
	        cll_reinicia = conn.prepareCall("{call inicializa_test}");
	        cll_reinicia.execute();

	        realizar_donacion("inexistente", 1, 0.3f, new Date());

	        System.out.println("TEST DONACION 2 MAL");

	    } catch (SQLException e) {
	        System.out.println("TEST DONACION 2 OK");

	    } finally {
	        if (cll_reinicia != null) cll_reinicia.close();
	        if (conn != null) conn.close();
	    }
	    
	    
	    // Test 3: El hospital no existe
		System.out.println("TEST DONACION 3: El hospital no existe");

	    try {
	        conn = pool.getConnection();
	        cll_reinicia = conn.prepareCall("{call inicializa_test}");
	        cll_reinicia.execute();

	        realizar_donacion("12345678A", 999, 0.3f, new Date());

	        System.out.println("TEST DONACION 3 MAL");

	    } catch (SQLException e) {
	        System.out.println("TEST DONACION 3 OK");

	    } finally {
	        if (cll_reinicia != null) cll_reinicia.close();
	        if (conn != null) conn.close();
	    }
	    
	    
	    // Test 4: Se exceden 0.45L de sangre
		System.out.println("TEST DONACION 4: Se exceden 0.45L de sangre");

	    try {
	        conn = pool.getConnection();
	        cll_reinicia = conn.prepareCall("{call inicializa_test}");
	        cll_reinicia.execute();

	        realizar_donacion("12345678A", 1, 0.6f, new Date());

	        System.out.println("TEST DONACION 4 MAL");

	    } catch (SQLException e) {
	        System.out.println("TEST DONACION 4 OK");

	    } finally {
	        if (cll_reinicia != null) cll_reinicia.close();
	        if (conn != null) conn.close();
	    }
	    
	    // Test 5: Cantidad negativa de sangre
		System.out.println("TEST DONACION 5: Cantidad negativa de sangre");

	    try {
	        conn = pool.getConnection();
	        cll_reinicia = conn.prepareCall("{call inicializa_test}");
	        cll_reinicia.execute();

	        realizar_donacion("12345678A", 1, -0.2f, new Date());

	        System.out.println("TEST DONACION 5 MAL");

	    } catch (SQLException e) {
	        System.out.println("TEST DONACION 5 OK");

	    } finally {
	        if (cll_reinicia != null) cll_reinicia.close();
	        if (conn != null) conn.close();
	    }
	    
	    
	    // Test 6: Segunda donacion antes de 15 dias
	    System.out.println("TEST DONACION 6: Segunda donacion antes de 15 dias");
	    
	    try {
	        conn = pool.getConnection();
	        cll_reinicia = conn.prepareCall("{call inicializa_test}");
	        cll_reinicia.execute();

	        // Primera donacion correcta
	        realizar_donacion("12345678A", 1, 0.3f, java.sql.Date.valueOf("2025-01-10"));

	        // Segunda donacion (solo 5 dias despues)
	        realizar_donacion("12345678A", 1, 0.3f, java.sql.Date.valueOf("2025-01-15"));

	        System.out.println("TEST DONACION 6 MAL");

	    } catch (SQLException e) {
	        System.out.println("TEST DONACION 6 OK");

	    } finally {
	        if (cll_reinicia != null) cll_reinicia.close();
	        if (conn != null) conn.close();
	    }

	}
}
