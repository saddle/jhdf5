/****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of HDF Java Products. The full HDF Java copyright       *
 * notice, including terms governing use, modification, and redistribution,  *
 * is contained in the file, COPYING.  COPYING can be found at the root of   *
 * the source code distribution tree. You can also access it online  at      *
 * http://www.hdfgroup.org/products/licenses.html.  If you do not have       *
 * access to the file, you may request a copy from help@hdfgroup.org.        *
 ****************************************************************************/

package ncsa.hdf.hdf5lib;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import ncsa.hdf.hdf5lib.callbacks.H5D_iterate_cb;
import ncsa.hdf.hdf5lib.callbacks.H5D_iterate_t;
import ncsa.hdf.hdf5lib.callbacks.H5L_iterate_cb;
import ncsa.hdf.hdf5lib.callbacks.H5L_iterate_t;
import ncsa.hdf.hdf5lib.callbacks.H5O_iterate_cb;
import ncsa.hdf.hdf5lib.callbacks.H5O_iterate_t;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5JavaException;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.hdf5lib.structs.H5AC_cache_config_t;
import ncsa.hdf.hdf5lib.structs.H5A_info_t;
import ncsa.hdf.hdf5lib.structs.H5G_info_t;
import ncsa.hdf.hdf5lib.structs.H5L_info_t;
import ncsa.hdf.hdf5lib.structs.H5O_info_t;

/**
 * This class is the Java interface for the HDF5 library.
 * <p>
 * This code is the called by Java programs to access the entry points of the
 * HDF5 library. Each routine wraps a single HDF5 entry point, generally
 * with the arguments and return codes analogous to the C interface.
 * <p>
 * For details of the HDF5 library, see the HDF5 Documentation at: <a
 * href="http://hdfgroup.org/HDF5/">http://hdfgroup.org/HDF5/</a>
 * <hr>
 * <p>
 * <b>Mapping of arguments for Java</b>
 * 
 * <p>
 * In general, arguments to the HDF Java API are straightforward translations
 * from the 'C' API described in the HDF Reference Manual.
 * <p>
 * 
 * <center>
 * <table border=2 cellpadding=2>
 * <caption><b>HDF-5 C types to Java types</b> </caption>
 * <tr>
 * <td><b>HDF-5</b></td>
 * <td><b>Java</b></td>
 * </tr>
 * <tr>
 * <td>H5T_NATIVE_INT</td>
 * <td>int, Integer</td>
 * </tr>
 * <tr>
 * <td>H5T_NATIVE_SHORT</td>
 * <td>short, Short</td>
 * </tr>
 * <tr>
 * <td>H5T_NATIVE_FLOAT</td>
 * <td>float, Float</td>
 * </tr>
 * <tr>
 * <td>H5T_NATIVE_DOUBLE</td>
 * <td>double, Double</td>
 * </tr>
 * <tr>
 * <td>H5T_NATIVE_CHAR</td>
 * <td>byte, Byte</td>
 * </tr>
 * <tr>
 * <td>H5T_C_S1</td>
 * <td>java.lang.String</td>
 * </tr>
 * <tr>
 * <td>void * <BR>
 * (i.e., pointer to `Any')</td>
 * <td>Special -- see HDFArray</td>
 * </tr>
 * </table>
 * </center>
 * <p>
 * <center> <b>General Rules for Passing Arguments and Results</b> </center>
 * <p>
 * In general, arguments passed <b>IN</b> to Java are the analogous basic types,
 * as above. The exception is for arrays, which are discussed below.
 * <p>
 * The <i>return value</i> of Java methods is also the analogous type, as above.
 * A major exception to that rule is that all HDF functions that return
 * SUCCEED/FAIL are declared <i>boolean</i> in the Java version, rather than
 * <i>int</i> as in the C. Functions that return a value or else FAIL are
 * declared the equivalent to the C function. However, in most cases the Java
 * method will raise an exception instead of returning an error code. See <a
 * href="#ERRORS">Errors and Exceptions</a> below.
 * <p>
 * Java does not support pass by reference of arguments, so arguments that are
 * returned through <b>OUT</b> parameters must be wrapped in an object or array.
 * The Java API for HDF consistently wraps arguments in arrays.
 * <p>
 * For instance, a function that returns two integers is declared:
 * <p>
 * 
 * <pre>
 *       h_err_t HDF5dummy( int *a1, int *a2)
 * </pre>
 * 
 * For the Java interface, this would be declared:
 * <p>
 * 
 * <pre>
 * public synchronized static native int HDF5dummy(int args[]);
 * </pre>
 * 
 * where <i>a1</i> is <i>args[0]</i> and <i>a2</i> is <i>args[1]</i>, and would
 * be invoked:
 * <p>
 * 
 * <pre>
 * H5.HDF5dummy(a);
 * </pre>
 * 
 * <p>
 * All the routines where this convention is used will have specific
 * documentation of the details, given below.
 * <p>
 * <a NAME="ARRAYS"> <b>Arrays</b> </a>
 * <p>
 * HDF5 needs to read and write multi-dimensional arrays of any number type (and
 * records). The HDF5 API describes the layout of the source and destination,
 * and the data for the array passed as a block of bytes, for instance,
 * <p>
 * 
 * <pre>
 *      herr_t H5Dread(int fid, int filetype, int memtype, int memspace,
 *      void * data);
 * </pre>
 * 
 * <p>
 * where ``void *'' means that the data may be any valid numeric type, and is a
 * contiguous block of bytes that is the data for a multi-dimensional array. The
 * other parameters describe the dimensions, rank, and datatype of the array on
 * disk (source) and in memory (destination).
 * <p>
 * For Java, this ``ANY'' is a problem, as the type of data must always be
 * declared. Furthermore, multidimensional arrays are definitely <i>not</i>
 * layed out contiguously in memory. It would be infeasible to declare a
 * separate routine for every combination of number type and dimensionality. For
 * that reason, the <a
 * href="./ncsa.hdf.hdf5lib.HDFArray.html"><b>HDFArray</b></a> class is used to
 * discover the type, shape, and size of the data array at run time, and to
 * convert to and from a contiguous array of bytes in synchronized static native
 * C order.
 * <p>
 * The upshot is that any Java array of numbers (either primitive or sub-classes
 * of type <b>Number</b>) can be passed as an ``Object'', and the Java API will
 * translate to and from the appropriate packed array of bytes needed by the C
 * library. So the function above would be declared:
 * <p>
 * 
 * <pre>
 * public synchronized static native int H5Dread(int fid, int filetype,
 *         int memtype, int memspace, Object data);
 * </pre>
 * 
 * and the parameter <i>data</i> can be any multi-dimensional array of numbers,
 * such as float[][], or int[][][], or Double[][].
 * <p>
 * <a NAME="CONSTANTS"> <b>HDF-5 Constants</b>
 * <p>
 * The HDF-5 API defines a set of constants and enumerated values. Most of these
 * values are available to Java programs via the class <a
 * href="./ncsa.hdf.hdf5lib.HDF5Constants.html"> <b>HDF5Constants</b></a>. For
 * example, the parameters for the h5open() call include two numeric values,
 * <b><i>HDFConstants.H5F_ACC_RDWR</i></b> and
 * <b><i>HDF5Constants.H5P_DEFAULT</i></b>. As would be expected, these numbers
 * correspond to the C constants <b><i>H5F_ACC_RDWR</i></b> and
 * <b><i>H5P_DEFAULT</i></b>.
 * <p>
 * The HDF-5 API defines a set of values that describe number types and sizes,
 * such as "H5T_NATIVE_INT" and "hsize_t". These values are determined at run
 * time by the HDF-5 C library. To support these parameters, the Java class <a
 * href="./ncsa.hdf.hdf5lib.HDF5CDataTypes.html"> <b>HDF5CDataTypes</b></a>
 * looks up the values when initiated. The values can be accessed as public
 * variables of the Java class, such as:
 * 
 * <pre>
 * int data_type = HDF5CDataTypes.JH5T_NATIVE_INT;
 * </pre>
 * 
 * The Java application uses both types of constants the same way, the only
 * difference is that the <b><i>HDF5CDataTypes</i></b> may have different values
 * on different platforms.
 * <p>
 * <a NAME="ERRORS"> <b>Error handling and Exceptions</b>
 * <p>
 * The HDF5 error API (H5E) manages the behavior of the error stack in the HDF-5
 * library. This API is omitted from the JHI5. Errors are converted into Java
 * exceptions. This is totally different from the C interface, but is very
 * natural for Java programming.
 * <p>
 * The exceptions of the JHI5 are organized as sub-classes of the class <a
 * href="./ncsa.hdf.hdf5lib.exceptions.HDF5Exception.html">
 * <b>HDF5Exception</b></a>. There are two subclasses of <b>HDF5Exception</b>,
 * <a href="./ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException.html">
 * <b>HDF5LibraryException</b></a> and <a
 * href="./ncsa.hdf.hdf5lib.exceptions.HDF5JavaException.html">
 * <b>HDF5JavaException</b></a>. The sub-classes of the former represent errors
 * from the HDF-5 C library, while sub-classes of the latter represent errors in
 * the JHI5 wrapper and support code.
 * <p>
 * The super-class <b><i>HDF5LibraryException</i></b> implements the method
 * '<b><i>printStackTrace()</i></b>', which prints out the HDF-5 error stack, as
 * described in the HDF-5 C API <i><b>H5Eprint()</b>.</i> This may be used by
 * Java exception handlers to print out the HDF-5 error stack.
 * <hr>
 * 
 * @version HDF5 1.2 <BR>
 *          <b>See also: <a href ="./ncsa.hdf.hdf5lib.HDFArray.html"> </b>
 *          ncsa.hdf.hdf5lib.HDFArray</a><BR>
 *          <a href ="./ncsa.hdf.hdf5lib.HDF5Constants.html"> </b>
 *          ncsa.hdf.hdf5lib.HDF5Constants</a><BR>
 *          <a href ="./ncsa.hdf.hdf5lib.HDF5CDataTypes.html"> </b>
 *          ncsa.hdf.hdf5lib.HDF5CDataTypes</a><BR>
 *          <a href ="./ncsa.hdf.hdf5lib.HDF5Exception.html">
 *          ncsa.hdf.hdf5lib.HDF5Exception<BR>
 *          <a href="http://hdfgroup.org/HDF5/">
 *          http://hdfgroup.org/HDF5"</a>
 **/
public class H5 implements java.io.Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 6129888282117053288L;

    /**
     * The version number of the HDF5 library: <br />
     * LIB_VERSION[0]: The major version of the library.<br />
     * LIB_VERSION[1]: The minor version of the library.<br />
     * LIB_VERSION[2]: The release number of the library.<br />
     * 
     * Make sure to update the verions number when a different library is used.
     */
    public final static int LIB_VERSION[] = { 1, 8, 10 };

    public final static String H5PATH_PROPERTY_KEY = "ncsa.hdf.hdf5lib.H5.hdf5lib";

    // add system property to load library by name from library path, via
    // System.loadLibrary()
    public final static String H5_LIBRARY_NAME_PROPERTY_KEY = "ncsa.hdf.hdf5lib.H5.loadLibraryName";

    /** logging level: 0 -- information, 1 -- warning message, 2 -- failure */
    public static int LOGGING_LEVEL = 2;
    
    private static Logger s_logger;
    private static String s_libraryName;
    private static boolean isLibraryLoaded = false;
    
    private final static boolean IS_CRITICAL_PINNING = true;
    
    private final static Vector<Integer> OPEN_IDS = new Vector<Integer>();

    static {
        loadH5Lib();
    }

    public static void loadH5Lib() {
        // Make sure that the library is loaded only once
        if (isLibraryLoaded)
            return;

        // use default logger, since spanning sources
        s_logger = Logger.getLogger("ncsa.hdf.hdf5lib"); 
        if (LOGGING_LEVEL == 2)
            s_logger.setLevel(Level.SEVERE);
        else if (LOGGING_LEVEL == 1)
            s_logger.setLevel(Level.WARNING);
        else
            s_logger.setLevel(Level.INFO);
        
        // first try loading library by name from user supplied library path
        s_libraryName = System.getProperty(H5_LIBRARY_NAME_PROPERTY_KEY, null);
        String mappedName = null;
        if ((s_libraryName != null) && (s_libraryName.length() > 0)) {
            try {
                mappedName = System.mapLibraryName(s_libraryName);
                System.loadLibrary(s_libraryName);
                isLibraryLoaded = true;
            }
            catch (Throwable err) {
                err.printStackTrace();
                isLibraryLoaded = false;
            }
            finally {
                s_logger.log(Level.INFO, "HDF5 library: " + s_libraryName
                        + " resolved to: " + mappedName + "; "
                        + (isLibraryLoaded ? "" : " NOT")
                        + " successfully loaded from java.library.path");

            }
        }

        if (!isLibraryLoaded) {
            // else try loading library via full path
            String filename = System.getProperty(H5PATH_PROPERTY_KEY, null);
            if ((filename != null) && (filename.length() > 0)) {
                File h5dll = new File(filename);
                if (h5dll.exists() && h5dll.canRead() && h5dll.isFile()) {
                    try {
                        System.load(filename);
                        isLibraryLoaded = true;
                    }
                    catch (Throwable err) {
                        err.printStackTrace();
                        isLibraryLoaded = false;
                    }
                    finally {
                        s_logger.log(Level.INFO, "HDF5 library: " + filename
                                + (isLibraryLoaded ? "" : " NOT")
                                + " successfully loaded.");

                    }
                }
                else {
                    isLibraryLoaded = false;
                    throw (new UnsatisfiedLinkError("Invalid HDF5 library, "
                            + filename));
                }
            }
        }

        // else load standard library
        if (!isLibraryLoaded) {
            try {
                s_libraryName = "jhdf5";
                mappedName = System.mapLibraryName(s_libraryName);
                System.loadLibrary("jhdf5");
                isLibraryLoaded = true;
            }
            catch (Throwable err) {
                err.printStackTrace();
                isLibraryLoaded = false;
            }
            finally {
                s_logger.log(Level.INFO, "HDF5 library: " + s_libraryName
                        + " resolved to: " + mappedName + "; "
                        + (isLibraryLoaded ? "" : " NOT")
                        + " successfully loaded from java.library.path");

            }
        }

        /* Important! Exit quietly */
        try {
            H5.H5dont_atexit();
        }
        catch (HDF5LibraryException e) {
            System.exit(1);
        }

        /* Important! Disable error output to C stdout */
        H5.H5error_off();

        /*
         * Optional: confirm the version This will crash immediately if not the
         * specified version.
         */
        Integer majnum = Integer
                .getInteger("ncsa.hdf.hdf5lib.H5.hdf5maj", null);
        Integer minnum = Integer
                .getInteger("ncsa.hdf.hdf5lib.H5.hdf5min", null);
        Integer relnum = Integer
                .getInteger("ncsa.hdf.hdf5lib.H5.hdf5rel", null);
        if ((majnum != null) && (minnum != null) && (relnum != null)) {
            H5.H5check_version(majnum.intValue(), minnum.intValue(), relnum
                    .intValue());
        }
    }

// ////////////////////////////////////////////////////////////
//                                                           //
// H5: General Library Functions                             //
//                                                           //
// ////////////////////////////////////////////////////////////

/**
 * Get number of open IDs.
 */
public final static int getOpenIDCount()
{
    return OPEN_IDS.size();
}

/**
 * Get the open ID at the specified index.
 * 
 * @param index -- an index of the open ID.
 * @return Returns the open ID at the specified index.
 */
public final static int getOpenID(int index)
{
    int id = -1;
    if (index >= 0 && index < OPEN_IDS.size())
        id = OPEN_IDS.elementAt(index);
    
    return id;
}

/**
 * H5check_version verifies that the arguments match the version numbers
 * compiled into the library.
 * 
 * @param majnum
 *            The major version of the library.
 * @param minnum
 *            The minor version of the library.
 * @param relnum
 *            The release number of the library.
 * @return a non-negative value if successful. Upon failure (when the
 *         versions do not match), this function causes the application to
 *         abort (i.e., crash)
 * 
 *         See C API function: herr_t H5check_version()
 **/
public synchronized static native int H5check_version(int majnum,
        int minnum, int relnum);

/**
 * H5close flushes all data to disk, closes all file identifiers, and cleans
 * up all memory used by the library.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5close() throws HDF5LibraryException;

/**
 * H5open initialize the library.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5open() throws HDF5LibraryException;

/**
 * H5dont_atexit indicates to the library that an atexit() cleanup routine
 * should not be installed. In order to be effective, this routine must be
 * called before any other HDF function calls, and must be called each time
 * the library is loaded/linked into the application (the first time and
 * after it's been un-loaded).
 * <P>
 * This is called by the static initializer, so this should never need to be
 * explicitly called by a Java program.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
private synchronized static native int H5dont_atexit()
        throws HDF5LibraryException;

/**
 * Turn off error handling By default, the C library prints the error stack
 * of the HDF-5 C library on stdout. This behavior may be disabled by
 * calling H5error_off().
 */
public synchronized static native int H5error_off();

/**
 * H5garbage_collect collects on all free-lists of all types.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5garbage_collect()
        throws HDF5LibraryException;

/**
 * H5get_libversion retrieves the major, minor, and release numbers of the
 * version of the HDF library which is linked to the application.
 * 
 * @param libversion
 *            The version information of the HDF library.
 * 
 *            <pre>
 *      libversion[0] = The major version of the library.
 *      libversion[1] = The minor version of the library.
 *      libversion[2] = The release number of the library.
 * </pre>
 * @return a non-negative value if successful, along with the version
 *         information.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5get_libversion(int[] libversion)
        throws HDF5LibraryException;

public synchronized static native int H5set_free_list_limits(
        int reg_global_lim, int reg_list_lim, int arr_global_lim,
        int arr_list_lim, int blk_global_lim, int blk_list_lim)
        throws HDF5LibraryException;


//////////////////////////////////////////////////////////////
////
//H5A: HDF5 1.8 Attribute Interface API Functions           //
////
//////////////////////////////////////////////////////////////

/**
 * H5Aclose terminates access to the attribute specified by its identifier,
 * attr_id.
 * 
 * @param attr_id
 *            IN: Attribute to release access to.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Aclose(int attr_id) throws HDF5LibraryException
{
    if (attr_id < 0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(attr_id);
    return _H5Aclose(attr_id);
}

private synchronized static native int _H5Aclose(int attr_id)
        throws HDF5LibraryException;

/**
 * H5Acopy copies the content of one attribute to another.
 * 
 * @param src_aid
 *            the identifier of the source attribute
 * @param dst_aid
 *            the identifier of the destinaiton attribute
 */
public synchronized static native int H5Acopy(int src_aid, int dst_aid)
        throws HDF5LibraryException;

/**
* H5Acreate creates an attribute which is attached to the object specified
* with loc_id.
* 
* @deprecated As of HDF5 1.8, replaced by {@link #H5Acreate( int, String, int, int, int, int) }
* 
* @param loc_id
*            IN: Object (dataset, group, or named datatype) to be attached
*            to.
* @param name
*            IN: Name of attribute to create.
* @param type_id
*            IN: Identifier of datatype for attribute.
* @param space_id
*            IN: Identifier of dataspace for attribute.
* @param create_plist
*            IN: Identifier of creation property list (currently not used).
* 
* @return an attribute identifier if successful
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
* @exception NullPointerException
*                - name is null.
**/
@Deprecated
public static int H5Acreate(int loc_id, String name, int type_id,
     int space_id, int create_plist)
     throws HDF5LibraryException, NullPointerException
{
 int id = _H5Acreate(loc_id, name, type_id, space_id, create_plist);
 if (id > 0)
     OPEN_IDS.addElement(id);
 return id;
}

private synchronized static native int _H5Acreate(int loc_id, String name,
     int type_id, int space_id, int create_plist)
     throws HDF5LibraryException, NullPointerException;

/**
* H5Acreate creates an attribute, attr_name, which is attached to the object specified by the identifier loc_id.  
* 
* @param loc_id            IN: Location or object identifier; may be dataset or group 
* @param attr_name          IN: Attribute name 
* @param type_id           IN: Attribute datatype identifier 
* @param space_id           IN: Attribute dataspace identifier
* @param acpl_id           IN: Attribute creation property list identifier 
* @param aapl_id           IN: Attribute access property list identifier 
* 
* @return  An attribute identifier if successful; otherwise returns a negative value. 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - Name is null.
**/
public static int H5Acreate( int loc_id, String attr_name, int type_id, int space_id, int acpl_id, int aapl_id )
throws HDF5LibraryException, NullPointerException
{
    int id = _H5Acreate2(loc_id, attr_name, type_id, space_id, acpl_id, aapl_id );
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

/**
* H5Acreate2 an attribute, attr_name, which is attached to the object
* specified by the identifier loc_id.
* 
* @see public static int H5Acreate( int loc_id, String attr_name, int
*      type_id, int space_id, int acpl_id, int aapl_id )
**/
private synchronized static native int _H5Acreate2( int loc_id, String attr_name, int type_id, int space_id, int acpl_id, int aapl_id ) 
        throws HDF5LibraryException, NullPointerException;

/**
* H5Acreate_by_name creates an attribute, attr_name, which is attached to the object specified by loc_id and obj_name.
*
* @param loc_id                IN: Location or object identifier; may be dataset or group
* @param obj_name            IN: Name, relative to loc_id, of object that attribute is to be attached to
* @param attr_name            IN: Attribute name 
* @param type_id            IN: Attribute datatype identifier
* @param space_id             IN: Attribute dataspace identifier 
* @param acpl_id             IN: Attribute creation property list identifier (currently not used).
* @param aapl_id             IN: Attribute access property list identifier (currently not used).
* @param lapl_id            IN: Link access property list 
*
* @return  An attribute identifier if successful; otherwise returns a negative value.
*
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - name is null.
**/
public static int H5Acreate_by_name(int loc_id, String obj_name, String attr_name, int type_id, int space_id, int acpl_id, int aapl_id, int lapl_id) 
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Acreate_by_name(loc_id, obj_name, attr_name, type_id, space_id, acpl_id, aapl_id, lapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Acreate_by_name(int loc_id, String obj_name, String attr_name, int type_id, int space_id, 
        int acpl_id, int aapl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Adelete removes the attribute specified by its name, name, from a
 * dataset, group, or named datatype.
 * 
 * @param loc_id
 *            IN: Identifier of the dataset, group, or named datatype.
 * @param name
 *            IN: Name of the attribute to delete.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native int H5Adelete(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Adelete_by_idx removes an attribute, specified by its location in an index, from an object.
*
*  @param loc_id             IN: Location or object identifier; may be dataset or group 
*  @param obj_name            IN: Name of object, relative to location, from which attribute is to be removed 
*  @param idx_type            IN: Type of index  
*  @param order                IN: Order in which to iterate over index
*  @param n                    IN: Offset within index  
*  @param lapl_id            IN: Link access property list identifier 
*
*  @return none
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - obj_name is null.
**/
public synchronized static native void H5Adelete_by_idx(int loc_id, String obj_name, int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Adelete_by_name removes the attribute attr_name from an object specified by location and name, loc_id and obj_name, respectively. 
*
* @param loc_id                IN: Location or object identifier; may be dataset or group
* @param obj_name            IN: Name of object, relative to location, from which attribute is to be removed
* @param attr_name            IN: Name of attribute to delete
* @param lapl_id            IN: Link access property list identifier.
*
* @return a non-negative value if successful; otherwise returns a negative value.
*
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - name is null.
**/
public synchronized static native int H5Adelete_by_name(int loc_id, String obj_name, String attr_name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Aexists determines whether the attribute attr_name exists on the object specified by obj_id.
*
* @param obj_id                IN: Object identifier.
* @param attr_name            IN: Name of the attribute.
*
* @return boolean true if an attribute with a given name exists.
*
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - attr_name is null.
**/
public synchronized static native boolean H5Aexists(int obj_id, String attr_name)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Aexists_by_name determines whether the attribute attr_name exists on an object. That object is specified by its location and name, 
* loc_id and obj_name, respectively.
*
* @param loc_id                IN: Location of object to which attribute is attached .
* @param obj_name            IN: Name, relative to loc_id, of object that attribute is attached to.
* @param attr_name            IN: Name of attribute.
* @param lapl_id            IN: Link access property list identifier.
*
* @return boolean true if an attribute with a given name exists, otherwise returns false.
*
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - name is null.
**/
public synchronized static native boolean H5Aexists_by_name(int obj_id, String obj_name, String attr_name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Aget_info retrieves attribute information, by attribute identifier. 
 * 
 * @param attr_id            IN: Attribute identifier 
 * 
 * @return  A buffer(H5A_info_t) for Attribute information 
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native H5A_info_t H5Aget_info(int attr_id)
        throws HDF5LibraryException;

/**
* H5Aget_info_by_idx Retrieves attribute information, by attribute index position. 
* 
* @param loc_id            IN: Location of object to which attribute is attached 
* @param obj_name        IN: Name of object to which attribute is attached, relative to location
* @param idx_type        IN: Type of index 
* @param order            IN: Index traversal order
* @param n                 IN: Attribute's position in index 
* @param lapl_id        IN: Link access property list
*  
* @return  A buffer(H5A_info_t) for Attribute information 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - obj_name is null.
**/
public synchronized static native H5A_info_t H5Aget_info_by_idx(int loc_id, String obj_name, int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Aget_info_by_name Retrieves attribute information, by attribute name. 
* 
* @param loc_id            IN: Location of object to which attribute is attached 
* @param obj_name        IN: Name of object to which attribute is attached, relative to location
* @param attr_name        IN: Attribute name
* @param lapl_id        IN: Link access property list
*  
* @return  A buffer(H5A_info_t) for Attribute information 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - obj_name is null.
**/
public synchronized static native H5A_info_t H5Aget_info_by_name(int loc_id, String obj_name, String attr_name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Aget_name retrieves the name of an attribute specified by the
 * identifier, attr_id.
 * 
 * @param attr_id
 *            IN: Identifier of the attribute.
 * @param buf_size
 *            IN: The size of the buffer to store the name in.
 * @param name
 *            OUT: Buffer to store name in.
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                JNI error writing back array
 * @exception ArrayStoreException
 *                JNI error writing back array
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 * @exception IllegalArgumentException
 *                - bub_size <= 0.
 * 
 * @return the length of the attribute's name if successful.
 **/
public synchronized static native long H5Aget_name(int attr_id,
        long buf_size, String[] name)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException,
        HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

public static long H5Aget_name(int attr_id, String[] name)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException,
        HDF5LibraryException, NullPointerException,
        IllegalArgumentException
{
    long len = H5Aget_name(attr_id, 0, null);
    
    return H5Aget_name(attr_id, len+1, name);
}

/**
* H5Aget_name_by_idx retrieves the name of an attribute that is attached to an object, which is specified by its location and name, 
* loc_id and obj_name, respectively.
* 
* @param attr_id            IN: Attribute identifier 
* @param obj_name            IN: Name of object to which attribute is attached, relative to location  
* @param idx_type            IN: Type of index 
* @param order                IN: Index traversal order  
* @param n                    IN: Attribute's position in index
* @param lapl_id            IN: Link access property list 
* 
* @return  String for Attribute name. 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - obj_name is null.
**/
public synchronized static native String H5Aget_name_by_idx(int loc_id, String obj_name, int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Aget_num_attrs returns the number of attributes attached to the object
 * specified by its identifier, loc_id.
 * 
 * @deprecated As of HDF5 1.8,   replaced by {@link #H5Oget_info( int )}
 * 
 * @param loc_id
 *            IN: Identifier of a group, dataset, or named datatype.
 * 
 * @return the number of attributes if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
@Deprecated
public synchronized static native int H5Aget_num_attrs(int loc_id)
        throws HDF5LibraryException;

/**
 * H5Aget_space retrieves a copy of the dataspace for an attribute.
 * 
 * @param attr_id
 *            IN: Identifier of an attribute.
 * 
 * @return attribute dataspace identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Aget_space(int attr_id) throws HDF5LibraryException
{
    int id = _H5Aget_space(attr_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Aget_space(int attr_id)
        throws HDF5LibraryException;

/**
* H5Aget_storage_size returns the amount of storage that is required for the specified attribute, attr_id.
* 
* @param attr_id            IN: Identifier of the attribute to query.
* 
* @return the amount of storage size allocated for the attribute; otherwise returns 0 (zero)
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public synchronized static native long H5Aget_storage_size(int attr_id)
        throws HDF5LibraryException;

/**
 * H5Aget_type retrieves a copy of the datatype for an attribute.
 * 
 * @param attr_id
 *            IN: Identifier of an attribute.
 * 
 * @return a datatype identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Aget_type(int attr_id) throws HDF5LibraryException
{
    int id = _H5Aget_type(attr_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Aget_type(int attr_id)
        throws HDF5LibraryException;

/**
 * H5Aopen opens an existing attribute, attr_name, that is attached to an object specified an object identifier, object_id.
 * 
 * @param obj_id            IN: Identifer for object to which attribute is attached 
 * @param attr_name          IN: Name of attribute to open  
 * @param aapl_id           IN: Attribute access property list identifier 
 * 
 * @return  An attribute identifier if successful; otherwise returns a negative value. 
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - Name is null.
 **/
public static int H5Aopen(int obj_id, String attr_name, int aapl_id)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Aopen(obj_id, attr_name, aapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Aopen(int obj_id, String attr_name, int aapl_id) 
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Aopen_by_idx opens an existing attribute that is attached to an object specified by location and name, loc_id and obj_name, respectively
 * 
 * @param loc_id            IN: Location of object to which attribute is attached  
 * @param obj_name          IN: Name of object to which attribute is attached, relative to location  
 * @param idx_type           IN: Type of index
 * @param order              IN: Index traversal order  
 * @param n                 IN: Attribute's position in index 
 * @param aapl_id           IN: Attribute access property list 
 * @param lapl_id           IN: Link access property list 
 * 
 * @return  An attribute identifier if successful; otherwise returns a negative value. 
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - Name is null.
 **/
public static int H5Aopen_by_idx(int loc_id, String obj_name, int idx_type, int order ,long n, int aapl_id, int lapl_id) 
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Aopen_by_idx(loc_id, obj_name, idx_type, order , n, aapl_id, lapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Aopen_by_idx(int loc_id, String obj_name, int idx_type, int order ,long n, int aapl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Aopen_by_name Opens an attribute for an object by object name and attribute name
*
*  @param loc_id             IN: Location from which to find object to which attribute is attached  
*  @param obj_name            IN: Name of object to which attribute is attached, relative to loc_id 
*  @param attr_name            IN: Name of attribute to open  
*  @param aapl_id            IN: Attribute access property list 
*  @param lapl_id            IN: Link access property list identifier 
*
*  @return Returns an attribute identifier if successful; otherwise returns a negative value. 
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - obj_name is null.
**/
public static int H5Aopen_by_name(int loc_id, String obj_name, String attr_name, int aapl_id, int lapl_id) 
throws HDF5LibraryException, NullPointerException
{
    int id = _H5Aopen_by_name(loc_id, obj_name, attr_name, aapl_id, lapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

/**
 * H5Aopen_idx opens an attribute which is attached to the object specified
 * with loc_id. The location object may be either a group, dataset, or named
 * datatype, all of which may have any sort of attribute.
 * 
 * @deprecated As of HDF5 1.8,  replaced by {@link #H5Aopen_by_idx(int, String, int, int, long, int, int)  }
 * 
 * @param loc_id
 *            IN: Identifier of the group, dataset, or named datatype
 *            attribute
 * @param idx
 *            IN: Index of the attribute to open.
 * 
 * @return attribute identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
@Deprecated
public static int H5Aopen_idx(int loc_id, int idx)
        throws HDF5LibraryException
{
    int id = _H5Aopen_idx(loc_id, idx);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Aopen_idx(int loc_id, int idx)
        throws HDF5LibraryException;

/**
 * H5Aopen_name opens an attribute specified by its name, name, which is
 * attached to the object specified with loc_id.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Aopen_by_name(int, String, String, int, int)}
 *  
 * @param loc_id
 *            IN: Identifier of a group, dataset, or named datatype
 *            atttribute
 * @param name
 *            IN: Attribute name.
 * 
 * @return attribute identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
@Deprecated
public static int H5Aopen_name(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Aopen_name(loc_id, name);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Aopen_name(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Aread reads an attribute, specified with attr_id. The attribute's
 * memory datatype is specified with mem_type_id. The entire attribute is
 * read into buf from the file.
 * 
 * @param attr_id
 *            IN: Identifier of an attribute to read.
 * @param mem_type_id
 *            IN: Identifier of the attribute datatype (in memory).
 * @param buf
 *            IN: Buffer for data to be read.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data buffer is null.
 **/
public synchronized static native int H5Aread(int attr_id, int mem_type_id,
        byte[] buf) throws HDF5LibraryException, NullPointerException;

/**
 * H5Aread reads an attribute, specified with attr_id. The attribute's
 * memory datatype is specified with mem_type_id. The entire attribute is
 * read into data object from the file.
 * 
 * @param attr_id
 *            IN: Identifier of an attribute to read.
 * @param mem_type_id
 *            IN: Identifier of the attribute datatype (in memory).
 * @param obj
 *            IN: Object for data to be read.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data buffer is null. See public synchronized static
 *                native int H5Aread( )
 **/
public synchronized static int H5Aread(int attr_id, int mem_type_id,
        Object obj) throws HDF5Exception, NullPointerException
{
    HDFArray theArray = new HDFArray(obj);
    byte[] buf = theArray.emptyBytes();

    // This will raise an exception if there is an error
    int status = H5Aread(attr_id, mem_type_id, buf);

    // No exception: status really ought to be OK
    if (status >= 0) {
        obj = theArray.arrayify(buf);
    }

    return status;
}

public synchronized static native int H5AreadVL(int attr_id,
        int mem_type_id, String[] buf)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Arename changes the name of attribute that is attached to the object specified by loc_id. 
* The attribute named old_attr_name is renamed new_attr_name.
* 
* @param loc_id         IN: Location or object identifier; may be dataset or group   
* @param old_attr_name  IN: Prior attribute name 
* @param new_attr_name  IN: New attribute name 
* 
* @return  A non-negative value if successful; otherwise returns a negative value. 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - Name is null.
**/
public synchronized static native int H5Arename(int loc_id, String old_attr_name, String new_attr_name)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Arename_by_name changes the name of attribute that is attached to the object specified by loc_id and obj_name. 
* The attribute named old_attr_name is renamed new_attr_name.
* 
* @param loc_id            IN: Location or object identifier; may be dataset or group   
* @param obj_name            IN: Name of object, relative to location, whose attribute is to be renamed  
* @param old_attr_name     IN: Prior attribute name 
* @param new_attr_name       IN: New attribute name 
* @param lapl_id           IN: Link access property list 
* 
* @return  A non-negative value if successful; otherwise returns a negative value. 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - Name is null.
**/
public synchronized static native int H5Arename_by_name(int loc_id, String obj_name, String old_attr_name, String new_attr_name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Awrite writes an attribute, specified with attr_id. The attribute's
 * memory datatype is specified with mem_type_id. The entire attribute is
 * written from buf to the file.
 * 
 * @param attr_id
 *            IN: Identifier of an attribute to write.
 * @param mem_type_id
 *            IN: Identifier of the attribute datatype (in memory).
 * @param buf
 *            IN: Data to be written.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data is null.
 **/
public synchronized static native int H5Awrite(int attr_id,
        int mem_type_id, byte[] buf)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Awrite writes an attribute, specified with attr_id. The attribute's
 * memory datatype is specified with mem_type_id. The entire attribute is
 * written from data object to the file.
 * 
 * @param attr_id
 *            IN: Identifier of an attribute to write.
 * @param mem_type_id
 *            IN: Identifier of the attribute datatype (in memory).
 * @param obj
 *            IN: Data object to be written.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data object is null. See public synchronized static
 *                native int H5Awrite(int attr_id, int mem_type_id, byte[]
 *                buf);
 **/
public synchronized static int H5Awrite(int attr_id, int mem_type_id,
        Object obj) throws HDF5Exception, NullPointerException
{
    HDFArray theArray = new HDFArray(obj);
    byte[] buf = theArray.byteify();

    int retVal = H5Awrite(attr_id, mem_type_id, buf);
    buf = null;
    theArray = null;
    return retVal;
}

//////////////////////////////////////////////////////////////
////
//H5D: Datasets Interface Functions //
////
//////////////////////////////////////////////////////////////

public synchronized static native int H5Dchdir_ext(String dir_name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Dclose ends access to a dataset specified by dataset_id and releases
 * resources used by it.
 * 
 * @param dataset_id
 *            Identifier of the dataset to finish access to.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Dclose(int dataset_id) throws HDF5LibraryException
{
    if (dataset_id < 0)
        throw new HDF5LibraryException("Negative ID");
    
    OPEN_IDS.removeElement(dataset_id);
    return _H5Dclose(dataset_id);
}

private synchronized static native int _H5Dclose(int dataset_id)
        throws HDF5LibraryException;

/**
 * H5Dcopy copies the content of one dataset to another dataset.
 * 
 * @param src_did
 *            the identifier of the source dataset
 * @param dst_did
 *            the identifier of the destinaiton dataset
 */
public synchronized static native int H5Dcopy(int src_did, int dst_did)
        throws HDF5LibraryException;

/**
* H5Dcreate creates a data set with a name, name, in the file or in the
* group specified by the identifier loc_id.
*
* @deprecated As of HDF5 1.8, replaced by {@link #H5Dcreate(int, String, int, int, int, int, int) }
* 
* @param loc_id
*            Identifier of the file or group to create the dataset within.
* @param name
*            The name of the dataset to create.
* @param type_id
*            Identifier of the datatype to use when creating the dataset.
* @param space_id
*            Identifier of the dataspace to use when creating the dataset.
* @param create_plist_id
*            Identifier of the set creation property list.
* 
* @return a dataset identifier if successful
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
* @exception NullPointerException
*                - name is null.
**/
@Deprecated
public static int H5Dcreate(int loc_id, String name, int type_id,
     int space_id, int create_plist_id)
     throws HDF5LibraryException, NullPointerException
{
 int id = _H5Dcreate(loc_id, name, type_id, space_id, create_plist_id);
 if (id > 0)
     OPEN_IDS.addElement(id);
 return id;
}

private synchronized static native int _H5Dcreate(int loc_id, String name,
     int type_id, int space_id, int create_plist_id)
     throws HDF5LibraryException, NullPointerException;

/**
 *  H5Dcreate creates a new dataset named name at the 
 *  location specified by loc_id.
 *
 *  @param loc_id   IN: Location identifier 
 *  @param name     IN: Dataset name
 *  @param type_id  IN: Datatype identifier
 *  @param space_id IN: Dataspace identifier 
 *  @param lcpl_id  IN: Identifier of link creation property list.
 *  @param dcpl_id  IN: Identifier of dataset creation property list.
 *  @param dapl_id  IN: Identifier of dataset access property list.
 *
 *  @return a dataset identifier
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public static int H5Dcreate(int loc_id, String name, int type_id,
        int space_id, int lcpl_id, int dcpl_id, int dapl_id)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Dcreate2(loc_id, name, type_id, space_id, lcpl_id, dcpl_id, dapl_id);
    if (id > 0)
      OPEN_IDS.addElement(id);
    return id;
}
/**
 *  H5Dcreate2 creates a new dataset named name at the 
 *  location specified by loc_id.
 *
 *  @see public static int H5Dcreate(int loc_id, String name, int type_id,
 *     int space_id, int lcpl_id, int dcpl_id, int dapl_id)
 **/
private synchronized static native int _H5Dcreate2(int loc_id, String name, int type_id,
        int space_id, int lcpl_id, int dcpl_id, int dapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Dcreate_anon creates a dataset in the file specified by loc_id. 
 *
 *  @param loc_id   IN: Location identifier 
 *  @param type_id  IN: Datatype identifier
 *  @param space_id IN: Dataspace identifier 
 *  @param lcpl_id  IN: Identifier of link creation property list.
 *  @param dcpl_id  IN: Identifier of dataset creation property list.
 *  @param dapl_id  IN: Identifier of dataset access property list.
 *
 *  @return a dataset identifier
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Dcreate_anon(int loc_id, int type_id, int space_id,
        int dcpl_id, int dapl_id)
        throws HDF5LibraryException
{
    int id = _H5Dcreate_anon(loc_id, type_id, space_id, dcpl_id, dapl_id);
    if (id > 0)
      OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Dcreate_anon(int loc_id, int type_id, int space_id,
        int dcpl_id, int dapl_id)
    throws HDF5LibraryException;

/**
 * H5Dextend verifies that the dataset is at least of size size.
 * 
 * @param dataset_id IN: Identifier of the dataset.
 * @param size       IN: Array containing the new magnitude of each dimension.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - size array is null.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Dset_extent(int, long[]) }
 **/
@Deprecated
public synchronized static native int H5Dextend(int dataset_id, byte[] size)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Dextend verifies that the dataset is at least of size size.
 * 
 * @param dataset_id IN: Identifier of the dataset.
 * @param size       IN: Array containing the new magnitude of each dimension.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - size array is null.
 * 
  * @deprecated As of HDF5 1.8, replaced by {@link #H5Dset_extent(int, long[]) }
 **/
@Deprecated
public synchronized static int H5Dextend(int dataset_id, long[] size)
        throws HDF5Exception, NullPointerException
{
    int rval = -1;
    HDFArray theArray = new HDFArray(size);
    byte[] buf = theArray.byteify();
    rval = H5Dextend(dataset_id, buf);
    buf = null;
    theArray = null;
    return rval;
}

/**
 *  H5Dfill explicitly fills the dataspace selection in memory, space_id, 
 *  with the fill value specified in fill. 
 *
 *  @param fill      IN: Pointer to the fill value to be used.
 *  @param fill_type IN: Fill value datatype identifier.
 *  @param buf   IN/OUT: Pointer to the memory buffer containing the selection to be filled.
 *  @param buf_type  IN: Datatype of dataspace elements to be filled.
 *  @param space     IN: Dataspace describing memory buffer and containing the selection to be filled.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native void H5Dfill(byte[] fill, int fill_type, byte[] buf, int buf_type, int space)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Dget_access_plist returns an identifier for a copy of the
 *  dataset access property list for a dataset.
 *
 *  @param dset_id IN: Identifier of the dataset to query.
 *
 *  @return a dataset access property list identifier
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Dget_access_plist(int dset_id)
        throws HDF5LibraryException;

/**
 * H5Dget_create_plist returns an identifier for a copy of the dataset
 * creation property list for a dataset.
 * 
 * @param dataset_id
 *            Identifier of the dataset to query.
 * @return a dataset creation property list identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Dget_create_plist(int dataset_id)
        throws HDF5LibraryException
{
    int id = _H5Dget_create_plist(dataset_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Dget_create_plist(int dataset_id)
        throws HDF5LibraryException;

/** H5Dget_offset returns the address in the file of the dataset dset_id.
 *
 *  @param dset_id  IN: Identifier of the dataset in question
 *
 *  @return the offset in bytes.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Dget_offset(int dset_id)
        throws HDF5LibraryException;

/**
 * H5Dget_space returns an identifier for a copy of the dataspace for a
 * dataset.
 * 
 * @param dataset_id
 *            Identifier of the dataset to query.
 * 
 * @return a dataspace identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Dget_space(int dataset_id) throws HDF5LibraryException
{
    int id = _H5Dget_space(dataset_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Dget_space(int dataset_id)
        throws HDF5LibraryException;

/**
 *  H5Dget_space_status determines whether space has been 
 *  allocated for the dataset dset_id. 
 *
 *  @param dset_id IN: Identifier of the dataset to query.
 *
 *  @return the space allocation status
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Dget_space_status(int dset_id)
        throws HDF5LibraryException;

/**
 *  H5Dget_space_status determines whether space has been 
 *  allocated for the dataset dset_id. 
 *
 *  @param dset_id IN: Identifier of the dataset to query.
 *
 *  @return the space allocation status
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Dget_space_status(int dset_id,
        int[] status) throws HDF5LibraryException, NullPointerException
{
    return _H5Dget_space_status(dset_id, status);
}
private synchronized static native int _H5Dget_space_status(int dset_id,
        int[] status) throws HDF5LibraryException, NullPointerException;

/**
 * H5Dget_storage_size returns the amount of storage that is required for
 * the dataset.
 * 
 * @param dataset_id
 *            Identifier of the dataset in question
 * 
 * @return he amount of storage space allocated for the dataset.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Dget_storage_size(int dataset_id)
        throws HDF5LibraryException, IllegalArgumentException;

/**
 * H5Dget_type returns an identifier for a copy of the datatype for a
 * dataset.
 * 
 * @param dataset_id
 *            Identifier of the dataset to query.
 * 
 * @return a datatype identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Dget_type(int dataset_id) throws HDF5LibraryException
{
    int id = _H5Dget_type(dataset_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Dget_type(int dataset_id)
        throws HDF5LibraryException;

public synchronized static native int H5Dgetdir_ext(String[] dir_name,
        int size) throws HDF5LibraryException, NullPointerException;

//// Define the operator function pointer for H5Diterate()
//public interface H5D_operator_t extends Callback {
//int callback(Pointer elem, int type_id, int ndim,
//       LongByReference point, Pointer operator_data);
//}

/**
 *  H5Diterate iterates over all the data elements in the memory buffer buf, 
 *  executing the callback function operator once for each such data element. 
 *
 *  @param buf     IN/OUT: Pointer to the memory containing the elements to iterate over.
 *  @param buf_type    IN: Buffer datatype identifier.
 *  @param space       IN: Dataspace describing memory buffer.
 *  @param op          IN: Callback function to operate on each value.
 *  @param op_data IN/OUT: Pointer to any user-efined data for use by operator function.
 *
 *  @return  returns the return value of the first operator that returns a positive value, or zero if all members were 
 *           processed with no operator returning non-zero.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native int H5Diterate(byte[] buff, int buf_type, int space,
        H5D_iterate_cb op, H5D_iterate_t op_data)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Dopen opens the existing dataset specified by a location identifier 
 *  and name, loc_id  and name, respectively. 
 *
 *  @deprecated As of HDF5 1.8, replaced by {@link #H5Dopen(int, String, int) }
 *
 *  @param loc_id   IN: Location identifier 
 *  @param name     IN: Dataset name
 *
 *  @return a dataset identifier if successful
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
@Deprecated
public static int H5Dopen(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Dopen(loc_id, name);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Dopen(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Dopen opens the existing dataset specified by a location identifier 
 *  and name, loc_id  and name, respectively. 
 *
 *  @param loc_id   IN: Location identifier 
 *  @param name     IN: Dataset name
 *  @param dapl_id  IN: Identifier of dataset access property list.
 *
 *  @return a dataset identifier if successful
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public static int H5Dopen(int loc_id, String name, int dapl_id)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Dopen2(loc_id, name, dapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
/**
 *  H5Dopen2 opens the existing dataset specified by a location identifier 
 *  and name, loc_id  and name, respectively. 
 *
 *  @see public static int H5Dopen(int loc_id, String name, int dapl_id)
 **/
private synchronized static native int _H5Dopen2(int loc_id, String name, int dapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Dread reads a (partial) dataset, specified by its identifier
 * dataset_id, from the file into the application memory buffer buf.
 * 
 * @param dataset_id
 *            Identifier of the dataset read from.
 * @param mem_type_id
 *            Identifier of the memory datatype.
 * @param mem_space_id
 *            Identifier of the memory dataspace.
 * @param file_space_id
 *            Identifier of the dataset's dataspace in the file.
 * @param xfer_plist_id
 *            Identifier of a transfer property list for this I/O operation.
 * @param buf
 *            Buffer to store data read from the file.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data buffer is null.
 **/
public synchronized static native int H5Dread(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, byte[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dread(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, byte[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dread(dataset_id, mem_type_id, mem_space_id, file_space_id,
            xfer_plist_id, buf, true);
}

public synchronized static int H5Dread(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, Object obj)
        throws HDF5Exception, HDF5LibraryException, NullPointerException
{
    return H5Dread(dataset_id, mem_type_id, mem_space_id, file_space_id,
            xfer_plist_id, obj, true);
}

/**
 * H5Dread reads a (partial) dataset, specified by its identifier
 * dataset_id, from the file into the application data object.
 * 
 * @param dataset_id
 *            Identifier of the dataset read from.
 * @param mem_type_id
 *            Identifier of the memory datatype.
 * @param mem_space_id
 *            Identifier of the memory dataspace.
 * @param file_space_id
 *            Identifier of the dataset's dataspace in the file.
 * @param xfer_plist_id
 *            Identifier of a transfer property list for this I/O operation.
 * @param obj
 *            Object to store data read from the file.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5Exception
 *                - Failure in the data conversion.
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data object is null.
 **/
public synchronized static int H5Dread(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, Object obj,
        boolean isCriticalPinning)
        throws HDF5Exception, HDF5LibraryException, NullPointerException
{
    int status = -1;
    boolean is1D = false;

    Class dataClass = obj.getClass();
    if (!dataClass.isArray()) {
        throw (new HDF5JavaException("H5Dread: data is not an array"));
    }

    String cname = dataClass.getName();
    is1D = (cname.lastIndexOf('[') == cname.indexOf('['));
    char dname = cname.charAt(cname.lastIndexOf("[") + 1);

    if (is1D && (dname == 'B')) {
        status = H5Dread(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (byte[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'S')) {
        status = H5Dread_short(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (short[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'I')) {
        status = H5Dread_int(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (int[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'J')) {
        status = H5Dread_long(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (long[]) obj);
    }
    else if (is1D && (dname == 'F')) {
        status = H5Dread_float(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (float[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'D')) {
        status = H5Dread_double(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (double[]) obj,
                isCriticalPinning);
    }
    else if (H5.H5Tequal(mem_type_id, HDF5Constants.H5T_STD_REF_DSETREG)) {
        status = H5Dread_reg_ref(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (String[]) obj);
    }
    else if (is1D && (dataClass.getComponentType() == String.class)) {
        // Rosetta Biosoftware - add support for
        // Strings (variable length)
        if (H5.H5Tis_variable_str(mem_type_id)) {
            status = H5DreadVL(dataset_id, mem_type_id, mem_space_id,
                    file_space_id, xfer_plist_id, (Object[]) obj);
        }
        else {
            status = H5Dread_string(dataset_id, mem_type_id, mem_space_id,
                    file_space_id, xfer_plist_id, (String[]) obj);
        }
    }
    else {
        // Create a data buffer to hold the data
        // into a Java Array
        HDFArray theArray = new HDFArray(obj);
        byte[] buf = theArray.emptyBytes();

        // will raise exception if read fails
        status = H5Dread(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, buf, isCriticalPinning);
        if (status >= 0) {
            // convert the data into a Java
            // Array */
            obj = theArray.arrayify(buf);
        }

        // clean up these: assign 'null' as hint
        // to gc() */
        buf = null;
        theArray = null;
    }

    return status;
}

public synchronized static native int H5Dread_double(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, double[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dread_double(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, double[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dread_double(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dread_float(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, float[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dread_float(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, float[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dread_float(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dread_int(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, int[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dread_int(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, int[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dread_int(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dread_long(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, long[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dread_long(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, long[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dread_long(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dread_reg_ref(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, String[] buf)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Dread_short(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, short[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dread_short(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, short[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dread_short(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dread_string(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, String[] buf)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5DreadVL(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, Object[] buf)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Dset_extent sets the current dimensions of the chunked dataset dset_id 
 *  to the sizes specified in size. 
 *
 *  @param dset_id  IN: Chunked dataset identifier.
 *  @param size     IN: Array containing the new magnitude of each dimension of the dataset. 
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - size is null.
 **/
public synchronized static native void H5Dset_extent(int dset_id, long size[])
        throws HDF5LibraryException, NullPointerException;

private synchronized static native int _H5Aopen_by_name(int loc_id, String obj_name, String attr_name,int aapl_id, int lapl_id)
throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Dvlen_get_buf_size(int dataset_id,
        int type_id, int space_id, int[] size) throws HDF5LibraryException;

/**
 *  H5Dvlen_get_buf_size determines the number of bytes required to store the VL data from 
 *  the dataset, using the space_id for the selection in the dataset on disk and the 
 *  type_id for the memory representation of the VL data in memory. 
 *
 *  @param dset_id  IN: Identifier of the dataset read from.
 *  @param type_id  IN: Identifier of the datatype.
 *  @param space_id IN: Identifier of the dataspace.
 *
 *  @return the size in bytes of the memory buffer required to store the VL data.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native long H5Dvlen_get_buf_size_long(int dset_id, int type_id, int space_id)
        throws HDF5LibraryException;
//int H5Dvlen_get_buf_size(int dset_id, int type_id, int space_id, LongByReference size);

/**
 * 
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - buf is null.
 **/
public synchronized static native int H5Dvlen_reclaim(int type_id,
        int space_id, int xfer_plist_id, byte[] buf)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Dwrite writes a (partial) dataset, specified by its identifier
 * dataset_id, from the application memory buffer buf into the file.
 * 
 * @param dataset_id
 *            Identifier of the dataset read from.
 * @param mem_type_id
 *            Identifier of the memory datatype.
 * @param mem_space_id
 *            Identifier of the memory dataspace.
 * @param file_space_id
 *            Identifier of the dataset's dataspace in the file.
 * @param xfer_plist_id
 *            Identifier of a transfer property list for this I/O operation.
 * @param buf
 *            Buffer with data to be written to the file.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native int H5Dwrite(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, byte[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dwrite(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, byte[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dwrite(dataset_id, mem_type_id, mem_space_id, file_space_id,
            xfer_plist_id, buf, true);
}

public synchronized static int H5Dwrite(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, Object obj)
        throws HDF5Exception, HDF5LibraryException, NullPointerException
{
    return H5Dwrite(dataset_id, mem_type_id, mem_space_id, file_space_id,
            xfer_plist_id, obj, true);
}

/**
 * H5Dwrite writes a (partial) dataset, specified by its identifier
 * dataset_id, from the application memory data object into the file.
 * 
 * @param dataset_id
 *            Identifier of the dataset read from.
 * @param mem_type_id
 *            Identifier of the memory datatype.
 * @param mem_space_id
 *            Identifier of the memory dataspace.
 * @param file_space_id
 *            Identifier of the dataset's dataspace in the file.
 * @param xfer_plist_id
 *            Identifier of a transfer property list for this I/O operation.
 * @param obj
 *            Object with data to be written to the file.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5Exception
 *                - Failure in the data conversion.
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - data object is null.
 **/
public synchronized static int H5Dwrite(int dataset_id, int mem_type_id,
        int mem_space_id, int file_space_id, int xfer_plist_id, Object obj,
        boolean isCriticalPinning)
        throws HDF5Exception, HDF5LibraryException, NullPointerException
{
    int status = -1;
    boolean is1D = false;

    Class dataClass = obj.getClass();
    if (!dataClass.isArray()) {
        throw (new HDF5JavaException("H5Dread: data is not an array"));
    }

    String cname = dataClass.getName();
    is1D = (cname.lastIndexOf('[') == cname.indexOf('['));
    char dname = cname.charAt(cname.lastIndexOf("[") + 1);

    if (is1D && (dname == 'B')) {
        status = H5Dwrite(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (byte[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'S')) {
        status = H5Dwrite_short(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (short[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'I')) {
        status = H5Dwrite_int(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (int[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'J')) {
        status = H5Dwrite_long(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (long[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'F')) {
        status = H5Dwrite_float(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (float[]) obj,
                isCriticalPinning);
    }
    else if (is1D && (dname == 'D')) {
        status = H5Dwrite_double(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (double[]) obj,
                isCriticalPinning);
    }

    // Rosetta Biosoftware - call into H5DwriteString
    // for variable length Strings
    else if ((H5.H5Tget_class(mem_type_id) == HDF5Constants.H5T_STRING)
            && H5.H5Tis_variable_str(mem_type_id) && dataClass.isArray()
            && (dataClass.getComponentType() == String.class) && is1D) {
        status = H5DwriteString(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, (String[]) obj);

    }
    else {
        HDFArray theArray = new HDFArray(obj);
        byte[] buf = theArray.byteify();

        /* will raise exception on error */
        status = H5Dwrite(dataset_id, mem_type_id, mem_space_id,
                file_space_id, xfer_plist_id, buf, isCriticalPinning);

        // clean up these: assign 'null' as hint to
        // gc() */
        buf = null;
        theArray = null;
    }

    return status;
}

public synchronized static native int H5Dwrite_double(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, double[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dwrite_double(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, double[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dwrite_double(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dwrite_float(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, float[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dwrite_float(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, float[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dwrite_float(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dwrite_int(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, int[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dwrite_int(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, int[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dwrite_int(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dwrite_long(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, long[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dwrite_long(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, long[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dwrite_long(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

public synchronized static native int H5Dwrite_short(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, short[] buf, boolean isCriticalPinning)
        throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Dwrite_short(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, short[] buf)
        throws HDF5LibraryException, NullPointerException
{
    return H5Dwrite_short(dataset_id, mem_type_id, mem_space_id,
            file_space_id, xfer_plist_id, buf, true);
}

/**
 * H5DwriteString writes a (partial) variable length String dataset,
 * specified by its identifier dataset_id, from the application memory
 * buffer buf into the file.
 * 
 * @param dataset_id
 *            Identifier of the dataset read from.
 * @param mem_type_id
 *            Identifier of the memory datatype.
 * @param mem_space_id
 *            Identifier of the memory dataspace.
 * @param file_space_id
 *            Identifier of the dataset's dataspace in the file.
 * @param xfer_plist_id
 *            Identifier of a transfer property list for this I/O operation.
 * @param buf
 *            Buffer with data to be written to the file.
 * 
 * @return a non-negative value if successful
 * 
 * @author contributed by Rosetta Biosoftware
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/

public synchronized static native int H5DwriteString(int dataset_id,
        int mem_type_id, int mem_space_id, int file_space_id,
        int xfer_plist_id, String[] buf)
        throws HDF5LibraryException, NullPointerException;

//////////////////////////////////////////////////////////////
////
//H5E: Error Stack //
////
//////////////////////////////////////////////////////////////

/**
* H5Eauto_is_v2 determines whether the error auto reporting function for an
* error stack conforms to the H5E_auto2_t typedef or the H5E_auto1_t
* typedef.
* 
* @param stack_id
*            IN: Error stack identifier.
* 
* @return boolean true if the error stack conforms to H5E_auto2_t and false
*         if it conforms to H5E_auto1_t.
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
**/
public synchronized static native boolean H5Eauto_is_v2(int err_stack)
     throws HDF5LibraryException;

/**
* H5Eclear clears the error stack for the current thread. H5Eclear can fail
* if there are problems initializing the library.
* <p>
* This may be used by exception handlers to assure that the error condition
* in the HDF-5 library has been reset.
* 
* @return Returns a non-negative value if successful
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
**/
public static int H5Eclear() throws HDF5LibraryException
{
 H5Eclear2(HDF5Constants.H5E_DEFAULT);
 return 0;
}

/**
 * H5Eclear clears the error stack specified by estack_id, or, if estack_id
 * is set to H5E_DEFAULT, the error stack for the current thread.
 * 
 * @param stack_id
 *            IN: Error stack identifier.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static void H5Eclear(int err_stack) throws HDF5LibraryException
{
    H5Eclear2(err_stack);
}

/**
 * H5Eclear2 clears the error stack specified by estack_id, or, if estack_id
 * is set to H5E_DEFAULT, the error stack for the current thread.
 * 
 * @see public static void H5Eclear(int err_stack)
 **/
public synchronized static native void H5Eclear2(int err_stack)
        throws HDF5LibraryException;

/**
 * H5Eclose_msg closes an error message identifier, which can be either a
 * major or minor message.
 * 
 * @param err_id
 *            IN: Error message identifier.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Eclose_msg(int err_id)
        throws HDF5LibraryException;

/**
 * H5Eclose_stack closes the object handle for an error stack and releases
 * its resources.
 * 
 * @param stack_id
 *            IN: Error stack identifier.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Eclose_stack(int stack_id)
        throws HDF5LibraryException;

/**
 * H5Ecreate_msg adds an error message to an error class defined by client
 * library or application program.
 * 
 * @param cls_id
 *            IN: Error class identifier.
 * @param msg_type
 *            IN: The type of the error message.
 * @param msg
 *            IN: The error message.
 * 
 * @return a message identifier
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - msg is null.
 **/
public synchronized static native int H5Ecreate_msg(int cls_id,
        int msg_type, String msg)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Ecreate_stack creates a new empty error stack and returns the new
 * stack's identifier.
 * 
 * @param none
 * 
 * @return an error stack identifier
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Ecreate_stack()
        throws HDF5LibraryException;

//int H5Eget_auto(H5E_auto1_t func, PointerByReference client_data);
//{
//return H5Eget_auto1(func, client_data);
//}
//int H5Eget_auto1(H5E_auto1_t func, PointerByReference client_data);
//int H5Eset_auto(H5E_auto1_t func, Pointer client_data);
//{
//return H5Eset_auto1(func, client_data);
//}
//int H5Eset_auto1(H5E_auto1_t func, Pointer client_data);

/**
 * H5Eget_class_name retrieves the name of the error class specified by the
 * class identifier.
 * 
 * @param class_id
 *            IN: Error class identifier.
 * 
 * @return the name of the error class
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native String H5Eget_class_name(int class_id)
        throws HDF5LibraryException, NullPointerException;

// long H5Eget_class_name(int class_id, String name, IntegerType size);

/**
 * H5Eget_current_stack copies the current error stack and returns an error
 * stack identifier for the new copy.
 * 
 * @param none
 * 
 * @return an error stack identifier
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Eget_current_stack()
        throws HDF5LibraryException;

/**
 * H5Eset_current_stack replaces the content of the current error stack with
 * a copy of the content of the error stack specified by estack_id.
 * 
 * @param stack_id
 *            IN: Error stack identifier.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Eset_current_stack(int stack_id)
        throws HDF5LibraryException;

/**
 * H5Eget_msg retrieves the error message including its length and type.
 * 
 * @param msg_id
 *            IN: Name of the error class.
 * @param type
 *            OUT: The type of the error message. Valid values are H5E_MAJOR
 *            and H5E_MINOR.
 * 
 * @return the error message
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native String H5Eget_msg(int msg_id,
        int[] type_list) throws HDF5LibraryException;

// long H5Eget_msg(int msg_id, H5E_TYPE type, String msg, IntegerType size);

//
///**
//* H5Eget_major returns a string that describes the error.
//*
//* @deprecated As of HDF5 1.8
//*
//* @param major IN: Major error number.
//*
//* @return string describing the error
//*
//* @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//public synchronized static native String H5Eget_major(int major)
//throws HDF5LibraryException;
//
///**
//* H5Eget_minor returns a string that describes the error.
//*
//* @deprecated As of HDF5 1.8
//*
//* @param minor IN: Error stack identifier.
//*
//* @return string describing the error
//*
//* @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//public synchronized static native String H5Eget_minor(int minor)
//throws HDF5LibraryException;
/**
 * H5Eget_num retrieves the number of error records in the error stack
 * specified by estack_id (including major, minor messages and description).
 * 
 * @param stack_id
 *            IN: Error stack identifier.
 * 
 * @return the number of error messages
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Eget_num(int stack_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Eprint1 prints the error stack specified by estack_id on the specified
 * stream, stream.
 * 
 * @param stream
 *            IN: File pointer, or stderr if null.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Eprint2(int, Object)}
 **/
@Deprecated
public synchronized static native void H5Eprint1(Object stream)
        throws HDF5LibraryException;

/**
 * H5Eprint2 prints the error stack specified by estack_id on the specified
 * stream, stream.
 * 
 * @param stack_id
 *            IN: Error stack identifier.If the identifier is H5E_DEFAULT,
 *            the current error stack will be printed.
 * @param stream
 *            IN: File pointer, or stderr if null.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Eprint2(int err_stack,
        Object stream) throws HDF5LibraryException;

//public static int H5Epush(String file, String func, int line,
//int maj_id, int min_id, String msg)
//{
//H5Epush1(file, func, line, maj_id, min_id, msg);
//}
//private synchronized static native int H5Epush1(String file, String func,
//int line,
//int maj_id, int min_id, String msg);
//public static int H5Epush(int err_stack, String file, String func, int
//line,
//int cls_id, int maj_id, int min_id, String msg, ...)
//{
//H5Epush2(err_stack, file, func, line, cls_id, maj_id, min_id, msg, ...);
//}
//public synchronized static native int H5Epush2(int err_stack, String
//file, String func, int line,
//int cls_id, int maj_id, int min_id, String msg, ...);

/**
* H5Epop deletes the number of error records specified in count from the
* top of the error stack specified by estack_id (including major, minor
* messages and description).
* 
* @param stack_id
*            IN: Error stack identifier.
* @param count
*            IN: Version of the client library or application to which the
*            error class belongs.
* 
* @return none
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
**/
public synchronized static native void H5Epop(int stack_id, long count)
     throws HDF5LibraryException;

/**
 * H5Eregister_class registers a client library or application program to
 * the HDF5 error API so that the client library or application program can
 * report errors together with HDF5 library.
 * 
 * @param cls_name
 *            IN: Name of the error class.
 * @param lib_name
 *            IN: Name of the client library or application to which the
 *            error class belongs.
 * @param version
 *            IN: Version of the client library or application to which the
 *            error class belongs.
 * 
 * @return a class identifier
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native int H5Eregister_class(String cls_name,
        String lib_name, String version)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Eunregister_class removes the error class specified by class_id.
 * 
 * @param class_id
 *            IN: Error class identifier.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Eunregister_class(int class_id)
        throws HDF5LibraryException;
//
//int H5Ewalk(H5E_direction_t direction, H5E_walk1_t func,
//Pointer client_data)
//{
//return H5Ewalk1(direction, func, client_data);
//}
//int H5Ewalk1(H5E_direction_t direction, H5E_walk1_t func,
//Pointer client_data);

////Error stack traversal callback function pointers
//public interface H5E_walk2_t extends Callback
//{
//int callback(int n, H5E_error2_t err_desc, Pointer client_data);
//}
//public interface H5E_auto2_t extends Callback
//{
//int callback(int estack, Pointer client_data);
//}

//int H5Ewalk(int err_stack, H5E_direction_t direction, H5E_walk2_t func,
//Pointer client_data)
//{
//return H5Ewalk2(err_stack, direction, func, client_data);
//}
//int H5Ewalk2(int err_stack, H5E_direction_t direction, H5E_walk2_t func,
//Pointer client_data);
//int H5Eget_auto(int estack_id, H5E_auto2_t func, PointerByReference
//client_data);
//{
//return H5Eget_auto2(estack_id, func, client_data);
//}
//int H5Eget_auto2(int estack_id, H5E_auto2_t func, PointerByReference
//client_data);
//int H5Eset_auto(int estack_id, H5E_auto2_t func, Pointer client_data);
//{
//return H5Eset_auto2(estack_id, func, client_data);
//}
//int H5Eset_auto2(int estack_id, H5E_auto2_t func, Pointer client_data);

//////////////////////////////////////////////////////////////
////
//H5F: File Interface Functions //
////
//////////////////////////////////////////////////////////////

/**
 * H5Fclose terminates access to an HDF5 file.
 * 
 * @param file_id
 *            Identifier of a file to terminate access to.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Fclose(int file_id) throws HDF5LibraryException
{
    if (file_id <0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(file_id);
    return _H5Fclose(file_id);
}

private synchronized static native int _H5Fclose(int file_id)
        throws HDF5LibraryException;

/**
* H5Fopen opens an existing file and is the primary function for accessing
* existing HDF5 files.
* 
* @param name
*            Name of the file to access.
* @param flags
*            File access flags.
* @param access_id
*            Identifier for the file access properties list.
* 
* @return a file identifier if successful
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
* @exception NullPointerException
*                - name is null.
**/
public static int H5Fopen(String name, int flags, int access_id)
     throws HDF5LibraryException, NullPointerException
{
 int id = _H5Fopen(name, flags, access_id);
 if (id > 0)
     OPEN_IDS.addElement(id);
 return id;
}

private synchronized static native int _H5Fopen(String name, int flags,
     int access_id) throws HDF5LibraryException, NullPointerException;

/**
 * H5Freopen reopens an HDF5 file.
 * 
 * @param file_id
 *            Identifier of a file to terminate and reopen access to.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @return a new file identifier if successful
 **/
public static int H5Freopen(int file_id) throws HDF5LibraryException
{
    int id = _H5Freopen(file_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Freopen(int file_id)
        throws HDF5LibraryException;

/**
 * H5Fcreate is the primary function for creating HDF5 files.
 * 
 * @param name
 *            Name of the file to access.
 * @param flags
 *            File access flags. Possible values include:
 *            <UL>
 *            <LI>
 *            H5F_ACC_RDWR Allow read and write access to file.</LI>
 *            <LI>
 *            H5F_ACC_RDONLY Allow read-only access to file.</LI>
 *            <LI>
 *            H5F_ACC_TRUNC Truncate file, if it already exists, erasing all
 *            data previously stored in the file.</LI>
 *            <LI>
 *            H5F_ACC_EXCL Fail if file already exists.</LI>
 *            <LI>
 *            H5F_ACC_DEBUG Print debug information.</LI>
 *            <LI>
 *            H5P_DEFAULT Apply default file access and creation properties.
 *            </LI>
 *            </UL>
 * 
 * @param create_id
 *            File creation property list identifier, used when modifying
 *            default file meta-data. Use H5P_DEFAULT for default access
 *            properties.
 * @param access_id
 *            File access property list identifier. If parallel file access
 *            is desired, this is a collective call according to the
 *            communicator stored in the access_id (not supported in Java).
 *            Use H5P_DEFAULT for default access properties.
 * 
 * @return a file identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public static int H5Fcreate(String name, int flags, int create_id,
        int access_id) throws HDF5LibraryException, NullPointerException
{
    int id = _H5Fcreate(name, flags, create_id, access_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Fcreate(String name, int flags,
        int create_id, int access_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Fflush causes all buffers associated with a file or object to be
 * immediately flushed (written) to disk without removing the data from the
 * (memory) cache.
 * <P>
 * After this call completes, the file (or object) is in a consistent state
 * and all data written to date is assured to be permanent.
 * 
 * @param object_id
 *            Identifier of object used to identify the file.
 *            <b>object_id</b> can be any object associated with the file,
 *            including the file itself, a dataset, a group, an attribute,
 *            or a named data type.
 * @param scope
 *            specifies the scope of the flushing action, in the case that
 *            the HDF-5 file is not a single physical file.
 *            <P>
 *            Valid values are:
 *            <UL>
 *            <LI>
 *            H5F_SCOPE_GLOBAL Flushes the entire virtual file.</LI>
 *            <LI>
 *            H5F_SCOPE_LOCAL Flushes only the specified file.</LI>
 *            </UL>
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Fflush(int object_id, int scope)
        throws HDF5LibraryException;

/**
 * H5Fget_access_plist returns the file access property list identifier of
 * the specified file.
 * 
 * @param file_id
 *            Identifier of file to get access property list of
 * 
 * @return a file access property list identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Fget_access_plist(int file_id)
        throws HDF5LibraryException
{
    int id = _H5Fget_access_plist(file_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Fget_access_plist(int file_id)
        throws HDF5LibraryException;

/**
 * H5Fget_create_plist returns a file creation property list identifier
 * identifying the creation properties used to create this file.
 * 
 * @param file_id
 *            Identifier of the file to get creation property list
 * 
 * @return a file creation property list identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Fget_create_plist(int file_id)
        throws HDF5LibraryException
{
    int id = _H5Fget_create_plist(file_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Fget_create_plist(int file_id)
        throws HDF5LibraryException;

public synchronized static native long H5Fget_filesize(int file_id)
        throws HDF5LibraryException;

/**
 * H5Fget_freespace returns the amount of space that is unused by any
 * objects in the file.
 * 
 * @param file_id
 *            IN: File identifier for a currently-open HDF5 file
 * 
 * @return the amount of free space in the file
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Fget_freespace(int file_id)
        throws HDF5LibraryException;

///**
//* H5Fget_info returns global information for the file associated with the
//* object identifier obj_id.
//*
//* @param obj_id IN: Object identifier for any object in the file.
//*
//* @return the structure containing global file information.
//*
//* @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//public synchronized static native H5F_info_t H5Fget_info(int obj_id)
//throws HDF5LibraryException, NullPointerException;
//int H5Fget_info(int obj_id, H5F_info_t file_info);

/**
 * H5Fget_intent retrieves the intended access mode flag passed with H5Fopen
 * when the file was opened.
 * 
 * @param file_id
 *            IN: File identifier for a currently-open HDF5 file
 * 
 * @return the intended access mode flag, as originally passed with H5Fopen.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Fget_intent(int file_id)
        throws HDF5LibraryException;

// int H5Fget_intent(int file_id, IntByReference intent);

///**
//* H5Fget_mdc_config loads the current metadata cache configuration into
//* the instance of H5AC_cache_config_t pointed to by the config_ptr
//parameter.
//*
//* @param file_id IN: Identifier of the target file
//* @param config_ptr IN/OUT: Pointer to the instance of
//H5AC_cache_config_t in which the current metadata cache configuration is
//to be reported.
//*
//* @return none
//*
//* @exception HDF5LibraryException - Error from the HDF-5 Library.
//* @exception NullPointerException - config_ptr is null.
//**/
//public synchronized static native void H5Fget_mdc_config(int file_id,
//H5AC_cache_config_t config_ptr)
//throws HDF5LibraryException, NullPointerException;
//
///**
//* H5Fset_mdc_config attempts to configure the file's metadata cache
//according to the configuration supplied.
//*
//* @param file_id IN: Identifier of the target file
//* @param config_ptr IN: Pointer to the instance of H5AC_cache_config_t
//containing the desired configuration.
//*
//* @return none
//*
//* @exception HDF5LibraryException - Error from the HDF-5 Library.
//* @exception NullPointerException - config_ptr is null.
//**/
//public synchronized static native int H5Fset_mdc_config(int file_id,
//H5AC_cache_config_t config_ptr)
//throws HDF5LibraryException, NullPointerException;

/**
* H5Fget_mdc_hit_rate queries the metadata cache of the target file to
* obtain its hit rate (cache hits / (cache hits + cache misses)) since the
* last time hit rate statistics were reset.
* 
* @param file_id
*            IN: Identifier of the target file.
* 
* @return the double in which the hit rate is returned.
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
**/
public synchronized static native double H5Fget_mdc_hit_rate(int file_id)
     throws HDF5LibraryException;

/**
 * H5Fget_mdc_size queries the metadata cache of the target file for the
 * desired size information.
 * 
 * @param file_id
 *            IN: Identifier of the target file.
 * @param metadata_cache
 *            OUT: Current metadata cache information
 *            <ul>
 *            <li>metadata_cache[0] = max_size_ptr // current cache maximum
 *            size</li>
 *            <li>metadata_cache[1] = min_clean_size_ptr // current cache
 *            minimum clean size</li>
 *            <li>metadata_cache[2] = cur_size_ptr // current cache size</li>
 *            </ul>
 * 
 * @return current number of entries in the cache
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - metadata_cache is null.
 **/
public synchronized static native int H5Fget_mdc_size(int file_id,
        long[] metadata_cache)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Fget_name retrieves the name of the file to which the object obj_id
 * belongs.
 * 
 * @param obj_id
 *            IN: Identifier of the object for which the associated filename
 *            is sought.
 * 
 * @return the filename.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native String H5Fget_name(int obj_id)
        throws HDF5LibraryException;

// long H5Fget_name(int obj_id, Buffer name/*out*/, long size);

public synchronized static native String H5Fget_name(int obj_id, int size)
        throws HDF5LibraryException;

/**
 * H5Fget_obj_count returns the number of open object identifiers for the
 * file.
 * 
 * @param file_id
 *            IN: File identifier for a currently-open HDF5 file
 * @param types
 *            IN: Type of object for which identifiers are to be returned.
 *            <ul>
 *            <li>H5F_OBJ_FILE Files only</li>
 *            <li>H5F_OBJ_DATASET Datasets only</li>
 *            <li>H5F_OBJ_GROUP Groups only</li>
 *            <li>H5F_OBJ_DATATYPE Named datatypes only</li>
 *            <li>H5F_OBJ_ATTR Attributes only</li>
 *            <li>H5F_OBJ_ALL All of the above</li>
 *            <li>H5F_OBJ_LOCAL Restrict search to objects opened through
 *            current file identifier.</li>
 *            </ul>
 * 
 * @return the number of open objects.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Fget_obj_count(int file_id,
        int types) throws HDF5LibraryException;

/**
 * H5Fget_obj_count returns the number of open object identifiers for the
 * file.
 * 
 * @param file_id
 *            IN: File identifier for a currently-open HDF5 file
 * @param types
 *            IN: Type of object for which identifiers are to be returned.
 *            <ul>
 *            <li>H5F_OBJ_FILE Files only</li>
 *            <li>H5F_OBJ_DATASET Datasets only</li>
 *            <li>H5F_OBJ_GROUP Groups only</li>
 *            <li>H5F_OBJ_DATATYPE Named datatypes only</li>
 *            <li>H5F_OBJ_ATTR Attributes only</li>
 *            <li>H5F_OBJ_ALL All of the above</li>
 *            <li>H5F_OBJ_LOCAL Restrict search to objects opened through
 *            current file identifier.</li>
 *            </ul>
 * 
 * @return the number of open objects.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Fget_obj_count_long(int file_id,
        int types) throws HDF5LibraryException;

/**
 * H5Fget_obj_ids returns the list of identifiers for all open HDF5 objects
 * fitting the specified criteria.
 * 
 * @param file_id
 *            IN: File identifier for a currently-open HDF5 file
 * @param types
 *            IN: Type of object for which identifiers are to be returned.
 * @param max_objs
 *            IN: Maximum number of object identifiers to place into
 *            obj_id_list.
 * @param obj_id_list
 *            OUT: Pointer to the returned list of open object identifiers.
 * 
 * @return the number of objects placed into obj_id_list.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - obj_id_list is null.
 **/
public synchronized static native int H5Fget_obj_ids(int file_id,
        int types, int max, int[] obj_id_list)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Fget_obj_ids returns the list of identifiers for all open HDF5 objects
 * fitting the specified criteria.
 * 
 * @param file_id
 *            IN: File identifier for a currently-open HDF5 file
 * @param types
 *            IN: Type of object for which identifiers are to be returned.
 * @param max_objs
 *            IN: Maximum number of object identifiers to place into
 *            obj_id_list.
 * @param obj_id_list
 *            OUT: Pointer to the returned list of open object identifiers.
 * 
 * @return the number of objects placed into obj_id_list.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - obj_id_list is null.
 **/
public synchronized static native long H5Fget_obj_ids_long(int file_id,
        int types, long max_objs, int[] obj_id_list)
        throws HDF5LibraryException, NullPointerException;

///**
//* H5Fget_vfd_handle returns a pointer to the file handle from the
//low-level file driver
//* currently being used by the HDF5 library for file I/O.
//*
//* @param file_id IN: Identifier of the file to be queried.
//* @param fapl IN: File access property list identifier.
//*
//* @return a pointer to the file handle being used by the low-level
//virtual file driver.
//*
//* @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//public synchronized static native Pointer file_handle
//H5Fget_vfd_handle(int file_id, int fapl)
//throws HDF5LibraryException;

/**
 * H5Fis_hdf5 determines whether a file is in the HDF5 format.
 * 
 * @param name
 *            File name to check format.
 * 
 * @return true if is HDF-5, false if not.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native boolean H5Fis_hdf5(String name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Fmount mounts the file specified by child_id onto the group specified
 * by loc_id and name using the mount properties plist_id.
 * 
 * @param loc_id
 *            The identifier for the group onto which the file specified by
 *            child_id is to be mounted.
 * @param name
 *            The name of the group onto which the file specified by
 *            child_id is to be mounted.
 * @param child_id
 *            The identifier of the file to be mounted.
 * @param plist_id
 *            The identifier of the property list to be used.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native int H5Fmount(int loc_id, String name,
        int child_id, int plist_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * Given a mount point, H5Funmount dissassociates the mount point's file
 * from the file mounted there.
 * 
 * @param loc_id
 *            The identifier for the location at which the specified file is
 *            to be unmounted.
 * @param name
 *            The name of the file to be unmounted.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native int H5Funmount(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Freset_mdc_hit_rate_stats resets the hit rate statistics counters in
 * the metadata cache associated with the specified file.
 * 
 * @param file_id
 *            IN: Identifier of the target file.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Freset_mdc_hit_rate_stats(
        int file_id) throws HDF5LibraryException;

// ////////////////////////////////////////////////////////////
// //
// H5G: Group Interface Functions //
// //
// ////////////////////////////////////////////////////////////

/**
 * H5Gclose releases resources used by a group which was opened by a call to
 * H5Gcreate() or H5Gopen().
 * 
 * @param group_id
 *            Group identifier to release.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Gclose(int group_id) throws HDF5LibraryException
{
    if (group_id < 0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(group_id);
    return _H5Gclose(group_id);
}

private synchronized static native int _H5Gclose(int group_id)
        throws HDF5LibraryException;

/**
 * H5Gcreate creates a new group with the specified name at the specified
 * location, loc_id.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Gcreate(int, String, int, int, int) }
 * 
 * @param loc_id
 *            The file or group identifier.
 * @param name
 *            The absolute or relative name of the new group.
 * @param size_hint
 *            An optional parameter indicating the number of bytes to
 *            reserve for the names that will appear in the group.
 * 
 * @return a valid group identifier for the open group if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
@Deprecated
public static int H5Gcreate(int loc_id, String name, long size_hint)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Gcreate(loc_id, name, size_hint);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Gcreate(int loc_id, String name,
        long size_hint) throws HDF5LibraryException, NullPointerException;

/**
 * H5Gcreate creates a new group with the specified name at the specified
 * location, loc_id.
 * 
 * @param loc_id
 *            IN: The file or group identifier.
 * @param name
 *            IN: The absolute or relative name of the new group.
 * @param lcpl_id
 *            IN: Identifier of link creation property list.
 * @param gcpl_id
 *            IN: Identifier of group creation property list.
 * @param gapl_id
 *            IN: Identifier of group access property list. (No group access
 *            properties have been implemented at this time; use
 *            H5P_DEFAULT.)
 * 
 * @return a valid group identifier
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public static int H5Gcreate(int loc_id, String name,
        int lcpl_id, int gcpl_id, int gapl_id)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Gcreate2(loc_id, name, lcpl_id, gcpl_id, gapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Gcreate2(int loc_id, String name,
        int lcpl_id, int gcpl_id, int gapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gcreate_anon creates a new empty group in the file specified by loc_id.
 * 
 * @param loc_id
 *            IN: File or group identifier specifying the file in which the
 *            new group is to be created.
 * @param gcpl_id
 *            IN: Identifier of group creation property list.
 * @param gapl_id
 *            IN: Identifier of group access property list. (No group access
 *            properties have been implemented at this time; use
 *            H5P_DEFAULT.)
 * 
 * @return a valid group identifier
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Gcreate_anon(int loc_id,
        int gcpl_id, int gapl_id) throws HDF5LibraryException
{
    int id = _H5Gcreate_anon(loc_id, gcpl_id, gapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Gcreate_anon(int loc_id,
        int gcpl_id, int gapl_id) throws HDF5LibraryException;

/**
 * H5Gget_comment retrieves the comment for the the object name. The comment
 * is returned in the buffer comment.
 * 
 * @param loc_id
 *            IN: Identifier of the file, group, dataset, or datatype.
 * @param name
 *            IN: Name of the object whose comment is to be set or reset.
 * @param bufsize
 *            IN: Anticipated size of the buffer required to hold comment.
 * @param comment
 *            OUT: The comment.
 * @return the number of characters in the comment, counting the null
 *         terminator, if successful
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                - JNI error writing back data
 * @exception ArrayStoreException
 *                - JNI error writing back data
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 * @exception IllegalArgumentException
 *                - size < 1, comment is invalid.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Oget_comment(int)}
 **/
@Deprecated
public synchronized static native int H5Gget_comment(int loc_id,
        String name, int bufsize, String[] comment)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException,
        HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Gset_comment sets the comment for the the object name to comment. Any
 * previously existing comment is overwritten.
 * 
 * @param loc_id
 *            IN: Identifier of the file, group, dataset, or datatype.
 * @param name
 *            IN: Name of the object whose comment is to be set or reset.
 * @param comment
 *            IN: The new comment.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name or comment is null.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Oset_comment(int, String)}
 **/
@Deprecated
public synchronized static native int H5Gset_comment(int loc_id,
        String name, String comment)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gget_create_plist returns an identifier for the group creation property
 * list associated with the group specified by group_id.
 * 
 * @param group_id
 *            IN: Identifier of the group.
 * 
 * @return an identifier for the group's creation property list
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Gget_create_plist(int group_id)
        throws HDF5LibraryException;

/**
 * H5Gget_info retrieves information about the group specified by group_id.
 * The information is returned in the group_info struct.
 * 
 * @param group_id
 *            IN: Identifier of the group.
 * 
 * @return a structure in which group information is returned
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native H5G_info_t H5Gget_info(int group_id)
        throws HDF5LibraryException;

// int H5Gget_info(int loc_id, H5G_info_t ginfo);

/**
 * H5Gget_info_by_idx retrieves information about a group, according to the
 * group's position within an index.
 * 
 * @param group_id
 *            IN: File or group identifier.
 * @param group_name
 *            IN: Name of group for which information is to be retrieved.
 * @param idx_type
 *            IN: Type of index by which objects are ordered
 * @param order
 *            IN: Order of iteration within index
 * @param n
 *            IN: Attribute's position in index
 * @param lapl_id
 *            IN: Link access property list.
 * 
 * @return a structure in which group information is returned
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native H5G_info_t H5Gget_info_by_idx(
        int group_id, String group_name, int idx_type, int order, long n,
        int lapl_id) throws HDF5LibraryException, NullPointerException;

// int H5Gget_info_by_idx(int group_id, String group_name,
// H5_index_t idx_type, H5_iter_order_t order, long n, H5G_info_t ginfo, int
// lapl_id);

/**
 * H5Gget_info_by_name retrieves information about the group group_name
 * located in the file or group specified by loc_id.
 * 
 * @param group_id
 *            IN: File or group identifier.
 * @param name
 *            IN: Name of group for which information is to be retrieved.
 * @param lapl_id
 *            IN: Link access property list.
 * 
 * @return a structure in which group information is returned
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native H5G_info_t H5Gget_info_by_name(
        int group_id, String name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

// int H5Gget_info_by_name(int group_id, String name, H5G_info_t ginfo, int
// lapl_id);

/**
 * H5Gget_linkval returns size characters of the link value through the
 * value argument if loc_id (a file or group identifier) and name specify a
 * symbolic link.
 * 
 * @param loc_id
 *            IN: Identifier of the file, group, dataset, or datatype.
 * @param name
 *            IN: Name of the object whose link value is to be checked.
 * @param size
 *            IN: Maximum number of characters of value to be returned.
 * @param value
 *            OUT: Link value.
 * 
 * @return a non-negative value, with the link value in value, if
 *         successful.
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                Copy back failed
 * @exception ArrayStoreException
 *                Copy back failed
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 * @exception IllegalArgumentException
 *                - size is invalid
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Lget_val(int, String, String[] , int)}
 **/
@Deprecated
public synchronized static native int H5Gget_linkval(int loc_id,
        String name, int size, String[] value)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException,
        HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * Returns number of objects in the group specified by its identifier
 * 
 * @param loc_id
 *            Identifier of the group or the file
 * @param num_obj
 *            Number of objects in the group
 * @return positive value if successful; otherwise returns a negative value.
 * @throws HDF5LibraryException
 * @throws NullPointerException
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Gget_info(int)}
 */
@Deprecated
public synchronized static native int H5Gget_num_objs(int loc_id,
        long[] num_obj) throws HDF5LibraryException, NullPointerException;

/**
 * retrieves information of all objects under the group (name) located in
 * the file or group specified by loc_id.
 * 
 * @param loc_id
 *            IN: File or group identifier
 * @param name
 *            IN: Name of group for which information is to be retrieved
 * @param objNames
 *            OUT: Names of all objects under the group, name.
 * @param objTypes
 *            OUT: Types of all objects under the group, name.
 * 
 * @return the number of items found
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 */
public synchronized static int H5Gget_obj_info_all(int loc_id, String name,
        String[] oname, int[] otype, long[] ref)
        throws HDF5LibraryException, NullPointerException
{
    if (oname == null) {
        throw new NullPointerException(
                "H5Gget_obj_info_all(): name array is null");
    }
    
   return H5Gget_obj_info_all(loc_id, name, oname, otype, null, null, ref, HDF5Constants.H5_INDEX_NAME);
}

public synchronized static int H5Gget_obj_info_all(int loc_id, String name,
        String[] oname, int[] otype, int[] ltype, long[] ref, int indx_type)
        throws HDF5LibraryException, NullPointerException
{
    return H5Gget_obj_info_full(loc_id, name, oname, otype, ltype, null, ref, indx_type, -1);
}

public synchronized static int H5Gget_obj_info_all(int loc_id, String name,
        String[] oname, int[] otype, int[] ltype, long[] fno, long[] ref, int indx_type)
        throws HDF5LibraryException, NullPointerException
{
    return H5Gget_obj_info_full(loc_id, name, oname, otype, ltype, fno, ref, oname.length, indx_type, -1);
}

public synchronized static int H5Gget_obj_info_full(int loc_id, String name,
        String[] oname, int[] otype, int[] ltype, long[] fno, long[] ref, int indx_type, int indx_order)
        throws HDF5LibraryException, NullPointerException
{
    if (oname == null) {
        throw new NullPointerException(
                "H5Gget_obj_info_full(): name array is null");
    }

    if (otype == null) {
        throw new NullPointerException(
                "H5Gget_obj_info_full(): object type array is null");
    }

    if (oname.length == 0) {
        throw new HDF5LibraryException(
                "H5Gget_obj_info_full(): array size is zero");
    }

    if (oname.length != otype.length) {
        throw new HDF5LibraryException(
                "H5Gget_obj_info_full(): name and type array sizes are different");
    }
    
    if (ltype == null)
        ltype = new int[otype.length];

    if (fno == null)
        fno = new long[ref.length];
    
    if (indx_type < 0)
        indx_type = HDF5Constants.H5_INDEX_NAME;
    
    if (indx_order < 0)
        indx_order = HDF5Constants.H5_ITER_INC;
    
    return H5Gget_obj_info_full(loc_id, name, oname, otype, ltype, fno, ref, oname.length, indx_type, indx_order);
}

private synchronized static native int H5Gget_obj_info_full(int loc_id,
        String name, String[] oname, int[] otype, int[] ltype, long[] fno, long[] ref, int n, int indx_type, int indx_order)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gget_obj_info_idx report the name and type of object with index 'idx'
 * in a Group. The 'idx' corresponds to the index maintained by H5Giterate.
 * Each link is returned, so objects with multiple links will be counted
 * once for each link.
 * 
 * @param loc_id
 *            IN: file or group ID.
 * @param name
 *            IN: name of the group to iterate, relative to the loc_id
 * @param idx
 *            IN: the index of the object to iterate.
 * @param oname
 *            the name of the object [OUT]
 * @param type
 *            the type of the object [OUT]
 * 
 * @return non-negative if successful, -1 if not.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 */
public synchronized static int H5Gget_obj_info_idx(int loc_id, String name,
        int idx, String[] oname, int[] type)
        throws HDF5LibraryException, NullPointerException
{
    long default_buf_size = 4096;
    String n[] = new String[1];
    n[0] = new String("");
    int grp_id = H5Gopen(loc_id, name);
    long val = H5Gget_objname_by_idx(grp_id, idx, n, default_buf_size);
    int type_code = H5Gget_objtype_by_idx(grp_id, idx);
    oname[0] = new String(n[0]);
    type[0] = type_code;
    int ret = (new Long(val)).intValue();
    return ret;
}

/*
 * //////////////////////////////////////////////////////////////////////////
 * /////// // // //Add these methods so that we don't need to call
 *  //in a loop to get information for all the object
 * in a group, which takes //a lot of time to finish if the number of
 * objects is more than 10,000 //
 * ///////////////////////////////////////////
 * //////////////////////////////////////
 */
/**
 * retrieves information of all objects (recurvisely) under the group (name)
 * located in the file or group specified by loc_id upto maximum specified
 * by objMax.
 * 
 * @param loc_id
 *            IN: File or group identifier
 * @param name
 *            IN: Name of group for which information is to be retrieved
 * @param objNames
 *            OUT: Names of all objects under the group, name.
 * @param objTypes
 *            OUT: Types of all objects under the group, name.
 * @param objMax
 *            IN: Maximum number of all objects under the group, name.
 * 
 * @return the number of items found
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 */
public synchronized static int H5Gget_obj_info_max(int loc_id,
        String[] objNames, int[] objTypes, int[] lnkTypes, long[] ref, int objMax)
        throws HDF5LibraryException, NullPointerException
{
    if (objNames == null) {
        throw new NullPointerException(
                "H5Gget_obj_info_max(): name array is null");
    }

    if (objTypes == null) {
        throw new NullPointerException(
                "H5Gget_obj_info_max(): object type array is null");
    }

    if (lnkTypes == null) {
        throw new NullPointerException(
                "H5Gget_obj_info_max(): link type array is null");
    }

    if (objNames.length <= 0) {
        throw new HDF5LibraryException(
                "H5Gget_obj_info_max(): array size is zero");
    }

    if (objMax <= 0) {
        throw new HDF5LibraryException(
                "H5Gget_obj_info_max(): maximum array size is zero");
    }

    if (objNames.length != objTypes.length) {
        throw new HDF5LibraryException(
                "H5Gget_obj_info_max(): name and type array sizes are different");
    }

    return H5Gget_obj_info_max(loc_id, objNames, objTypes, lnkTypes, ref, objMax,
            objNames.length);
}

private synchronized static native int H5Gget_obj_info_max(int loc_id,
        String[] oname, int[] otype, int[] ltype, long[] ref, int amax, int n)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gget_objinfo returns information about the specified object.
 * 
 * @param loc_id
 *            IN: File, group, dataset, or datatype identifier.
 * @param name
 *            IN: Name of the object for which status is being sought.
 * @param follow_link
 *            IN: Link flag.
 * @param fileno
 *            OUT: file id numbers.
 * @param objno
 *            OUT: object id numbers.
 * @param link_info
 *            OUT: link information.
 * 
 *            <pre>
 *          link_info[0] = nlink
 *          link_info[1] = type
 *          link_info[2] = linklen
 * </pre>
 * @param mtime
 *            OUT: modification time
 * 
 * @return a non-negative value if successful, with the fields of link_info
 *         and mtime (if non-null) initialized.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name or array is null.
 * @exception IllegalArgumentException
 *                - bad argument.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Lget_info(int, String, int) and #H5Oget_info(int)}
 **/
@Deprecated
public synchronized static native int H5Gget_objinfo(int loc_id,
        String name, boolean follow_link, long[] fileno, long[] objno,
        int[] link_info, long[] mtime)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Gget_objinfo returns information about the specified object in an
 * HDF5GroupInfo object.
 * 
 * @param loc_id
 *            IN: File, group, dataset, or datatype identifier.
 * @param name
 *            IN: Name of the object for which status is being sought.
 * @param follow_link
 *            IN: Link flag.
 * @param info
 *            OUT: the HDF5GroupInfo object to store the object infomation
 * 
 * @return a non-negative value if successful, with the fields of
 *         HDF5GroupInfo object (if non-null) initialized.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 * 
 * @see ncsa.hdf.hdf5lib.HDF5GroupInfo See public synchronized static native
 *      int H5Gget_objinfo();
 * 
 * @deprecated As of HDF5 1.8
 **/
@Deprecated
public synchronized static int H5Gget_objinfo(int loc_id, String name,
        boolean follow_link, HDF5GroupInfo info)
        throws HDF5LibraryException, NullPointerException
{
    int status = -1;
    long[] fileno = new long[2];
    long[] objno = new long[2];
    int[] link_info = new int[3];
    long[] mtime = new long[1];

    status = H5Gget_objinfo(loc_id, name, follow_link, fileno, objno,
            link_info, mtime);

    if (status >= 0) {
        info.setGroupInfo(fileno, objno, link_info[0], link_info[1],
                mtime[0], link_info[2]);
    }
    return status;
}

/**
 * Returns a name of an object specified by an index.
 * 
 * @param group_id
 *            Group or file identifier
 * @param idx
 *            Transient index identifying object
 * @param name
 *            the object name
 * @param size
 *            Name length
 * @return the size of the object name if successful, or 0 if no name is
 *         associated with the group identifier. Otherwise returns a
 *         negative value
 * @throws HDF5LibraryException
 * @throws NullPointerException
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Lget_name_by_idx(int, String, int, int, long, int)}
 */
@Deprecated
public synchronized static native long H5Gget_objname_by_idx(int group_id,
        long idx, String[] name, long size)
        throws HDF5LibraryException, NullPointerException;

/**
 * Returns the type of an object specified by an index.
 * 
 * @param group_id
 *            Group or file identifier.
 * @param idx
 *            Transient index identifying object.
 * @return Returns the type of the object if successful. Otherwise returns a
 *         negative value
 * @throws HDF5LibraryException
 * @throws NullPointerException
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Oget_info(int)}
 */
@Deprecated
public synchronized static native int H5Gget_objtype_by_idx(int group_id,
        long idx) throws HDF5LibraryException, NullPointerException;

/**
 * H5Glink creates a new name for an already existing object.
 * 
 * @param loc_id
 *            File, group, dataset, or datatype identifier.
 * @param link_type
 *            Link type. Possible values are:
 *            <UL>
 *            <LI>
 *            H5G_LINK_HARD</LI>
 *            <LI>
 *            H5G_LINK_SOFT.</LI>
 *            </UL>
 * @param current_name
 *            A name of the existing object if link is a hard link. Can be
 *            anything for the soft link.
 * @param new_name
 *            New name for the object.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - current_name or name is null. 
 * 
 * @deprecated As of HDF5 1.8, replaced by
 *             {@link #H5Lcreate_hard(int, String, int, String, int, int)
 *             and #H5Lcreate_soft(String, int, String, int, int) }
 **/
@Deprecated
public synchronized static native int H5Glink(int loc_id, int link_type,
        String current_name, String new_name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Glink creates a new name for an already existing object.
 * 
 * @deprecated As of HDF5 1.8
 **/
@Deprecated
public synchronized static native int H5Glink2(int curr_loc_id,
        String current_name, int link_type, int new_loc_id, String new_name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gunlink removes an association between a name and an object.
 * 
 * @param loc_id
 *            Identifier of the file containing the object.
 * @param name
 *            Name of the object to unlink.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Ldelete(int, String, int)}
 **/
@Deprecated
public synchronized static native int H5Gunlink(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gmove renames an object within an HDF5 file. The original name, src, is
 * unlinked from the group graph and the new name, dst, is inserted as an
 * atomic operation. Both names are interpreted relative to loc_id, which is
 * either a file or a group identifier.
 * 
 * @param loc_id
 *            File or group identifier.
 * @param src
 *            Object's original name.
 * @param dst
 *            Object's new name.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - src or dst is null.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Lmove(int, String, int,String, int, int)}
 **/
@Deprecated
public synchronized static native int H5Gmove(int loc_id, String src,
        String dst) throws HDF5LibraryException, NullPointerException;

// Backward compatibility:
// These functions have been replaced by new HDF5 library calls.
// The interface is preserved as a convenience to existing code.
/**
 * H5Gn_members report the number of objects in a Group. The 'objects'
 * include everything that will be visited by H5Giterate. Each link is
 * returned, so objects with multiple links will be counted once for each
 * link.
 * 
 * @param loc_id
 *            file or group ID.
 * @param name
 *            name of the group to iterate, relative to the loc_id
 * 
 * @return the number of members in the group or -1 if error.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 */
public synchronized static long H5Gn_members_long(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException 
{
    int grp_id = H5Gopen(loc_id, name);
    long n = -1;

    try { 
        H5G_info_t info = H5.H5Gget_info(grp_id);
        n =  info.nlinks;
    } finally {
        H5Gclose(grp_id); 
    } 
    
    return n;
}

/**
 * H5Gn_members report the number of objects in a Group. The 'objects'
 * include everything that will be visited by H5Giterate. Each link is
 * returned, so objects with multiple links will be counted once for each
 * link.
 * 
 * @param loc_id
 *            file or group ID.
 * @param name
 *            name of the group to iterate, relative to the loc_id
 * 
 * @return the number of members in the group or -1 if error.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 */
public synchronized static int H5Gn_members(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException 
{
    return (int) H5Gn_members_long(loc_id, name);
}

/**
 * H5Gopen opens an existing group with the specified name at the specified
 * location, loc_id.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Gopen(int, String, int) }
 * 
 * @param loc_id
 *            File or group identifier within which group is to be open.
 * @param name
 *            Name of group to open.
 * 
 * @return a valid group identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
@Deprecated
public static int H5Gopen(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Gopen(loc_id, name);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Gopen(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Gopen opens an existing group, name, at the location specified by
 * loc_id.
 * 
 * @param loc_id
 *            IN: File or group identifier specifying the location of the
 *            group to be opened.
 * @param name
 *            IN: Name of group to open.
 * @param gapl_id
 *            IN: Identifier of group access property list. (No group access
 *            properties have been implemented at this time; use
 *            H5P_DEFAULT.)
 * 
 * @return a valid group identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public static int H5Gopen(int loc_id, String name,
        int gapl_id) throws HDF5LibraryException, NullPointerException
{
    int id = _H5Gopen2(loc_id, name, gapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Gopen2(int loc_id, String name,
        int gapl_id) throws HDF5LibraryException, NullPointerException;

//////////////////////////////////////////////////////////////
////
//H5I: HDF5 1.8 Identifier Interface API Functions            //
////
//////////////////////////////////////////////////////////////

public synchronized static native int H5Iget_file_id(int obj_id)
        throws HDF5LibraryException;

public synchronized static native long H5Iget_name(int obj_id,
        String[] name, long size)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Iget_ref(int obj_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Idec_ref(int obj_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Iinc_ref(int obj_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Iget_type retrieves the type of the object identified by obj_id.
 * 
 * @param obj_id
 *            IN: Object identifier whose type is to be determined.
 * 
 * @return the object type if successful; otherwise H5I_BADID.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Iget_type(int obj_id)
        throws HDF5LibraryException;

/**
* H5Iget_type_ref retrieves the reference count on an ID type. The reference count is used by the library to indicate when an ID type can be destroyed. 
* 
* @param type          
*           IN: The identifier of the type whose reference count is to be retrieved
* 
* @return The current reference count on success, negative on failure.
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
**/
public synchronized static native int H5Iget_type_ref(int type)
        throws HDF5LibraryException;

/**
 * H5Inmembers returns the number of identifiers of the identifier type specified in type. 
 * 
 * @param type          
 *           IN: Identifier for the identifier type whose member count will be retrieved
 * 
 * @return  Number of identifiers of the specified identifier type
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Inmembers(int type)
        throws HDF5LibraryException;


// //////////////////////////////////////////////////////////////////
// //
// New APIs for HDF5Index //
// October 10, 2005 //
// //////////////////////////////////////////////////////////////////

public synchronized static native int H5INcreate(String grp_name,
        int grp_loc_id, int property_list, int data_loc_id,
        String data_loc_name, String field_name, long max_mem_size);

public synchronized static native int H5INquery(int dset_id, String keys[],
        Object ubounds, Object lbounds, int nkeys);

// //////////////////////////////////////////////////////////////////
// H5L: Link Interface Functions //
// //////////////////////////////////////////////////////////////////

/**
 *  H5Lcopy copies a link from one location to another. 
 *
 *  @param src_loc   IN: Location identifier of the source link 
 *  @param src_name  IN: Name of the link to be copied 
 *  @param dst_loc   IN: Location identifier specifying the destination of the copy 
 *  @param dst_name  IN: Name to be assigned to the new copy
 *  @param lcpl_id   IN: Link creation property list identifier
 *  @param lapl_id   IN: Link access property list identifier
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native void H5Lcopy(int src_loc, String src_name, int dst_loc,
        String dst_name, int lcpl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lcreate_external creates a new soft link to an external object, which is 
 *  an object in a different HDF5 file from the location of the link. 
 *
 *  @param file_name   IN: Name of the target file containing the target object.
 *  @param obj_name    IN: Path within the target file to the target object.
 *  @param link_loc_id IN: The file or group identifier for the new link. 
 *  @param link_name   IN: The name of the new link.
 *  @param lcpl_id     IN: Link creation property list identifier
 *  @param lapl_id     IN: Link access property list identifier
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native void H5Lcreate_external(String file_name, String obj_name,
        int link_loc_id, String link_name, int lcpl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lcreate_hard creates a new hard link to a pre-existing object in an HDF5 file.
 *
 *  @param cur_loc   IN: The file or group identifier for the target object.
 *  @param cur_name  IN: Name of the target object, which must already exist.
 *  @param dst_loc   IN: The file or group identifier for the new link.
 *  @param dst_name  IN: The name of the new link.
 *  @param lcpl_id   IN: Link creation property list identifier
 *  @param lapl_id   IN: Link access property list identifier
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - cur_name or dst_name is null.
 **/
public synchronized static native void H5Lcreate_hard(int cur_loc, String cur_name,
        int dst_loc, String dst_name, int lcpl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lcreate_soft creates a new soft link to an object in an HDF5 file.
 *
 *  @param link_target IN: Path to the target object, which is not required to exist.
 *  @param link_loc_id IN: The file or group identifier for the new link.
 *  @param link_name   IN: The name of the new link.
 *  @param lcpl_id     IN: Link creation property list identifier
 *  @param lapl_id     IN: Link access property list identifier
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - link_name is null.
 **/
public synchronized static native void H5Lcreate_soft(String link_target, int link_loc_id,
        String link_name, int lcpl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Ldelete removes the link specified from a group. 
 *
 *  @param loc_id  IN: Identifier of the file or group containing the object.
 *  @param name    IN: Name of the link to delete.
 *  @param lapl_id IN: Link access property list identifier
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native void H5Ldelete(int loc_id, String name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Ldelete_by_idx removes the nth link in a group according to the specified order 
 *  and in the specified index.
 *
 *  @param loc_id     IN: File or group identifier specifying location of subject group
 *  @param group_name IN: Name of subject group
 *  @param idx_type   IN: Index or field which determines the order 
 *  @param order      IN: Order within field or index
 *  @param n          IN: Link for which to retrieve information 
 *  @param lapl_id    IN: Link access property list identifier 
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - group_name is null.
 **/
public synchronized static native void H5Ldelete_by_idx(int loc_id, String group_name,
        int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lexists checks if a link with a particular name exists in a group. 
 *
 *  @param loc_id  IN: Identifier of the file or group to query. 
 *  @param name    IN: The name of the link to check. 
 *  @param lapl_id IN: Link access property list identifier
 *
 *  @return a boolean, true if the name exists, otherwise false.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native boolean H5Lexists(int loc_id, String name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lget_info returns information about the specified link.
 *
 *  @param loc_id  IN: Identifier of the file or group. 
 *  @param name    IN: Name of the link for which information is being sought.
 *  @param lapl_id IN: Link access property list identifier
 *
 *  @return a buffer(H5L_info_t) for the link information.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native H5L_info_t H5Lget_info(int loc_id, String name,
        int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lget_info_by_idx opens a named datatype at the location specified
 *  by loc_id and return an identifier for the datatype.
 *
 *  @param loc_id     IN: File or group identifier specifying location of subject group
 *  @param group_name IN: Name of subject group
 *  @param idx_type   IN: Type of index
 *  @param order      IN: Order within field or index
 *  @param n          IN: Link for which to retrieve information 
 *  @param lapl_id    IN: Link access property list identifier 
 *
 *  @return a buffer(H5L_info_t) for the link information.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - group_name is null.
 **/
public synchronized static native H5L_info_t H5Lget_info_by_idx(int loc_id, String group_name,
        int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lget_name_by_idx retrieves name of the nth link in a group, according to 
 *  the order within a specified field or index. 
 *
 *  @param loc_id     IN: File or group identifier specifying location of subject group
 *  @param group_name IN: Name of subject group
 *  @param idx_type   IN: Type of index
 *  @param order      IN: Order within field or index
 *  @param n          IN: Link for which to retrieve information 
 *  @param lapl_id    IN: Link access property list identifier 
 *
 *  @return a String for the link name.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - group_name is null.
 **/
public synchronized static native String H5Lget_name_by_idx(int loc_id, String group_name,
        int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lget_val returns the link value of a symbolic link.
 *
 *  @param loc_id      IN: Identifier of the file or group containing the object.
 *  @param name        IN: Name of the symbolic link.
 *  @param link_value OUT: Path of the symbolic link, or the file_name and path of an external file.
 *  @param lapl_id     IN: Link access property list identifier
 *
 *  @return the link type
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native int H5Lget_val(int loc_id, String name, String[] link_value, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lget_val_by_idx retrieves value of the nth link in a group, according to the order within an index. 
 *
 *  @param loc_id     IN: File or group identifier specifying location of subject group
 *  @param group_name IN: Name of subject group
 *  @param idx_type   IN: Type of index
 *  @param order      IN: Order within field or index
 *  @param n          IN: Link for which to retrieve information 
 *  @param link_value OUT: Path of the symbolic link, or the file_name and path of an external file.
 *  @param lapl_id    IN: Link access property list identifier 
 *
 *  @return the link type
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - group_name is null.
 **/
public synchronized static native int H5Lget_val_by_idx(int loc_id, String group_name,
        int idx_type, int order, long n, String[] link_value, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Literate iterates through links in a group. 
*
*  @param grp_id     IN: Identifier specifying subject group
*  @param idx_type   IN: Type of index  
*  @param order      IN: Order of iteration within index 
*  @param idx        IN: Iteration position at which to start  
*  @param op         IN: Callback function passing data regarding the link to the calling application  
*  @param op_data    IN: User-defined pointer to data required by the application for its processing of the link 
*
*  @return returns the return value of the first operator that returns a positive value, or zero if all members were 
*      processed with no operator returning non-zero.
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public synchronized static native int H5Literate(int grp_id, 
        int idx_type, int order,
        long idx, H5L_iterate_cb op, H5L_iterate_t op_data)
        throws HDF5LibraryException;

/**
*  H5Literate_by_name iterates through links in a group. 
*
*  @param grp_id     IN: Identifier specifying subject group
*  @param group_name IN: Name of subject group
*  @param idx_type   IN: Type of index  
*  @param order      IN: Order of iteration within index 
*  @param idx        IN: Iteration position at which to start  
*  @param op         IN: Callback function passing data regarding the link to the calling application  
*  @param op_data    IN: User-defined pointer to data required by the application for its processing of the link 
*  @param lapl_id    IN: Link access property list identifier 
*
*  @return returns the return value of the first operator that returns a positive value, or zero if all members were 
*    processed with no operator returning non-zero.
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - group_name is null.
**/
public synchronized static native int H5Literate_by_name(int loc_id, String group_name,
        int idx_type, int order, long idx,
        H5L_iterate_cb op, H5L_iterate_t op_data, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lmove renames a link within an HDF5 file.
 *
 *  @param src_loc   IN: Original file or group identifier.
 *  @param src_name  IN: Original link name.
 *  @param dst_loc   IN: Destination file or group identifier.
 *  @param dst_name  IN: New link name.
 *  @param lcpl_id   IN: Link creation property list identifier to be associated with the new link.
 *  @param lapl_id   IN: Link access property list identifier to be associated with the new link.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native void H5Lmove(int src_loc, String src_name, int dst_loc,
        String dst_name, int lcpl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Lvisit recursively visits all links starting from a specified group.
 *
 *  @param grp_id     IN: Identifier specifying subject group
 *  @param idx_type   IN: Type of index  
 *  @param order      IN: Order of iteration within index 
 *  @param op         IN: Callback function passing data regarding the link to the calling application  
 *  @param op_data    IN: User-defined pointer to data required by the application for its processing of the link 
 *
 *  @return returns the return value of the first operator that returns a positive value, or zero if all members were 
 *      processed with no operator returning non-zero.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
 public synchronized static native int H5Lvisit(int grp_id, int idx_type, int order,
         H5L_iterate_cb op, H5L_iterate_t op_data)
         throws HDF5LibraryException;

/**
 *  H5Lvisit_by_name recursively visits all links starting from a specified group. 
 *
 *  @param loc_id     IN: Identifier specifying subject group
 *  @param group_name IN: Name of subject group
 *  @param idx_type   IN: Type of index  
 *  @param order      IN: Order of iteration within index 
 *  @param op         IN: Callback function passing data regarding the link to the calling application  
 *  @param op_data    IN: User-defined pointer to data required by the application for its processing of the link 
 *
 *  @return returns the return value of the first operator that returns a positive value, or zero if all members were 
 *      processed with no operator returning non-zero.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - group_name is null.
 **/
 public synchronized static native int H5Lvisit_by_name(int loc_id, String group_name,
         int idx_type, int order, H5L_iterate_cb op,
         H5L_iterate_t op_data, int lapl_id)
         throws HDF5LibraryException, NullPointerException;

//////////////////////////////////////////////////////////////
////
//H5O: HDF5 1.8 Object Interface API Functions            //
////
//////////////////////////////////////////////////////////////

/**
*  H5Oclose closes the group, dataset, or named datatype specified.
*
*  @param object_id  IN: Object identifier 
*
*  @return none
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public static int H5Oclose(int object_id) throws HDF5LibraryException
{
    if (object_id < 0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(object_id);
    return _H5Oclose(object_id);
}

private synchronized static native int _H5Oclose(int object_id)
        throws HDF5LibraryException;



/**
*  H5Ocopy copies the group, dataset or named datatype specified from the file or 
*  group specified by source location to the destination location. 
*
*  @param src_loc_id  IN: Object identifier indicating the location of the source object to be copied 
*  @param src_name    IN: Name of the source object to be copied
*  @param dst_loc_id  IN: Location identifier specifying the destination  
*  @param dst_name    IN: Name to be assigned to the new copy 
*  @param ocpypl_id   IN: Object copy property list  
*  @param lcpl_id     IN: Link creation property list for the new hard link  
*
*  @return none
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native void H5Ocopy(int src_loc_id, String src_name, int dst_loc_id,
        String dst_name, int ocpypl_id, int lcpl_id)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Oget_comment retrieves the comment for the specified object.
*
*  @param obj_id  IN: File or group identifier 
*
*  @return the comment
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public synchronized static native String H5Oget_comment(int obj_id)
        throws HDF5LibraryException;

/**
*  H5Oset_comment sets the comment for the specified object.
*
*  @param obj_id  IN: Identifier of the target object
*  @param comment IN: The new comment.
*
*  @return none
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public synchronized static native void H5Oset_comment(int obj_id, String comment)
        throws HDF5LibraryException;

/**
*  H5Oget_comment_by_name retrieves the comment for an object.
*
*  @param loc_id  IN: Identifier of a file, group, dataset, or named datatype.
*  @param name    IN: Relative name of the object whose comment is to be set or reset.
*  @param lapl_id IN: Link access property list identifier. 
*
*  @return the comment
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native String H5Oget_comment_by_name(int loc_id, String name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;
//long H5Oget_comment_by_name(int loc_id, String name, String comment, long bufsize, int lapl_id);

/**
*  H5Oset_comment_by_name sets the comment for the specified object.
*
*  @param loc_id  IN: Identifier of a file, group, dataset, or named datatype.
*  @param name    IN: Relative name of the object whose comment is to be set or reset.
*  @param comment IN: The new comment.
*  @param lapl_id IN: Link access property list identifier. 
*
*  @return none
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native void H5Oset_comment_by_name(int loc_id, String name,
        String comment, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Oget_info retrieves the metadata for an object specified by an identifier. 
 *
 *  @param loc_id  IN: Identifier for target object 
 *
 *  @return object information
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native H5O_info_t H5Oget_info(int loc_id)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Oget_info_by_idx retrieves the metadata for an object, identifying the object by an index position. 
*
*  @param loc_id     IN: File or group identifier 
*  @param group_name IN: Name of group, relative to loc_id, in which object is located
*  @param idx_type   IN: Type of index by which objects are ordered  
*  @param order      IN: Order of iteration within index 
*  @param n          IN: Object to open 
*  @param lapl_id    IN: Access property list identifier for the link pointing to the object (Not currently used; pass as H5P_DEFAULT.)
*
*  @return object information
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native H5O_info_t H5Oget_info_by_idx(int loc_id, String group_name,
        int idx_type, int order, long n, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Oget_info_by_name retrieves the metadata for an object, identifying the object by location and relative name. 
*
*  @param loc_id  IN: File or group identifier specifying location of group in which object is located
*  @param name    IN: Relative name of group
*  @param lapl_id IN: Access property list identifier for the link pointing to the object (Not currently used; pass as H5P_DEFAULT.)
*
*  @return  object information
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native H5O_info_t H5Oget_info_by_name(int loc_id, String name, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Olink creates a new hard link to an object in an HDF5 file. 
*
*  @param obj_id      IN: Object to be linked.
*  @param new_loc_id  IN: File or group identifier specifying location at which object is to be linked. 
*  @param name        IN: Relative name of link to be created.
*  @param lcpl_id     IN: Link creation property list identifier. 
*  @param lapl_id     IN: Access property list identifier.
*
*  @return none
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native void H5Olink(int obj_id, int new_loc_id, String new_name,
        int lcpl_id, int lapl_id)
        throws HDF5LibraryException, NullPointerException;

///**
//*  H5Odecr_refcount decrements the hard link reference count for an object.
//*
//*  @param object_id  IN: Object identifier 
//*
//*  @return none
//*
//*  @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//
//public synchronized static native void H5Odecr_refcount(int object_id)
//      throws HDF5LibraryException;
///**
//*  H5Oincr_refcount increments the hard link reference count for an object.
//*
//*  @param object_id  IN: Object identifier 
//*
//*  @return none
//*
//*  @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//public synchronized static native void H5Oincr_refcount(int object_id)
//      throws HDF5LibraryException;

/**
*  H5Oopen opens a group, dataset, or named datatype specified by a location and a path name.
*
*  @param loc_id  IN: File or group identifier 
*  @param name    IN: Relative path to the object
*  @param lapl_id IN: Access property list identifier for the link pointing to the object 
*
*  @return an object identifier for the opened object
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public static int H5Oopen(int loc_id, String name, int lapl_id) 
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Oopen(loc_id, name, lapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Oopen(int loc_id, String name,
        int lapl_id) throws HDF5LibraryException, NullPointerException;

//
///**
//*  H5Oopen_by_addr opens a group, dataset, or named datatype using its address within an HDF5 file.
//*
//*  @param loc_id  IN: File or group identifier 
//*  @param addr    IN: Object's address in the file 
//*
//*  @return an object identifier for the opened object
//*
//*  @exception HDF5LibraryException - Error from the HDF-5 Library.
//**/
//public synchronized static native int H5Oopen_by_addr(int loc_id, long addr)
//      throws HDF5LibraryException;
//
///**
//*  H5Oopen_by_idx opens the nth object in the group specified.
//*
//*  @param loc_id     IN: File or group identifier 
//*  @param group_name IN: Name of group, relative to loc_id, in which object is located
//*  @param idx_type   IN: Type of index by which objects are ordered  
//*  @param order      IN: Order of iteration within index 
//*  @param n          IN: Object to open 
//*  @param lapl_id    IN: Access property list identifier for the link pointing to the object 
//*
//*  @return an object identifier for the opened object
//*
//*  @exception HDF5LibraryException - Error from the HDF-5 Library.
//*  @exception NullPointerException - group_name is null.
//**/
//public synchronized static native int H5Oopen_by_idx(int loc_id, String group_name,
//      H5_INDEX idx_type, H5_ITER order, long n, int lapl_id)
//      throws HDF5LibraryException, NullPointerException;

////Prototype for H5Ovisit/H5Ovisit_by_name() operator
//public interface H5O_iterate_t extends Callback
//{
//  int callback(int obj, String name, H5O_info_t info,
//      Pointer op_data);
//}


/**
*  H5Ovisit recursively visits all objects accessible from a specified object. 
*
*  @param obj_id     IN: Identifier of the object at which the recursive iteration begins.  
*  @param idx_type   IN: Type of index  
*  @param order      IN: Order of iteration within index 
*  @param op         IN: Callback function passing data regarding the object to the calling application  
*  @param op_data    IN: User-defined pointer to data required by the application for its processing of the object 
*
*  @return returns the return value of the first operator that returns a positive value, or zero if all members were 
     processed with no operator returning non-zero.
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native int H5Ovisit(int obj_id, int idx_type, int order,
      H5O_iterate_cb op, H5O_iterate_t op_data)
      throws HDF5LibraryException, NullPointerException;

/**
*  H5Ovisit_by_name recursively visits all objects starting from a specified object.
*
*  @param loc_id    IN: File or group identifier 
*  @param obj_name  IN: Relative path to the object
*  @param idx_type   IN: Type of index  
*  @param order      IN: Order of iteration within index 
*  @param op         IN: Callback function passing data regarding the object to the calling application  
*  @param op_data    IN: User-defined pointer to data required by the application for its processing of the object 
*  @param lapl_id   IN: Link access property list identifier
*
*  @return returns the return value of the first operator that returns a positive value, or zero if all members 
    were processed with no operator returning non-zero.
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public synchronized static native int H5Ovisit_by_name(int loc_id, String obj_name,
      int idx_type, int order, H5O_iterate_cb op,
      H5O_iterate_t op_data, int lapl_id)
      throws HDF5LibraryException, NullPointerException;

// ////////////////////////////////////////////////////////////
//                                                           //
// H5P: Property List Interface Functions                    //
//                                                           //
// ////////////////////////////////////////////////////////////

public synchronized static native boolean H5Pall_filters_avail(int dcpl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pclose terminates access to a property list.
 * 
 * @param plist
 *            IN: Identifier of the property list to terminate access to.
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Pclose(int plist) throws HDF5LibraryException
{
    if (plist < 0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(plist);
    return _H5Pclose(plist);
}

private synchronized static native int _H5Pclose(int plist)
        throws HDF5LibraryException;

/**
 * Closes an existing property list class
 * 
 * @param plid
 *            IN: Property list class to close
 * @return a non-negative value if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pclose_class(int plid)
        throws HDF5LibraryException;

/**
 * H5Pcopy copies an existing property list to create a new property list.
 * 
 * @param plist
 *            IN: Identifier of property list to duplicate.
 * 
 * @return a property list identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Pcopy(int plist) throws HDF5LibraryException
{
    int id = _H5Pcopy(plist);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Pcopy(int plist)
        throws HDF5LibraryException;

/**
 * H5Pcopy_prop copies a property from one property list or class to another
 * 
 * @param dst_id
 *            IN: Identifier of the destination property list or class
 * @param src_id
 *            IN: Identifier of the source property list or class
 * @param name
 *            IN: Name of the property to copy
 * @return a non-negative value if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pcopy_prop(int dst_id, int src_id,
        String name) throws HDF5LibraryException;

/**
 * H5Pcreate creates a new property as an instance of some property list
 * class.
 * 
 * @param type
 *            IN: The type of property list to create.
 * 
 * @return a property list identifier (plist) if successful; otherwise Fail
 *         (-1).
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Pcreate(int type) throws HDF5LibraryException
{
    int id = _H5Pcreate(type);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Pcreate(int type)
        throws HDF5LibraryException;

//hid_t H5Pcreate_class( hid_t parent_class, const char *name, H5P_cls_create_func_t create, void *create_data, H5P_cls_copy_func_t copy, void *copy_data, H5P_cls_close_func_t close, void *close_data ) 

/**
 * H5Pequal determines if two property lists or classes are equal
 * 
 * @param plid1
 *            IN: First property object to be compared
 * @param plid2
 *            IN: Second property object to be compared
 * @return positive value if equal; zero if unequal, a negative value if
 *         failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pequal(int plid1, int plid2)
        throws HDF5LibraryException;

public static boolean H5P_equal(int plid1, int plid2)
        throws HDF5LibraryException
{
    if(H5Pequal(plid1, plid2)==1) return true;
    return false;
}

/**
 * H5Pexist determines whether a property exists within a property list or
 * class
 * 
 * @param plid
 *            IN: Identifier for the property to query
 * @param name
 *            IN: Name of property to check for
 * @return a positive value if the property exists in the property object;
 *         zero if the property does not exist; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pexist(int plid, String name)
        throws HDF5LibraryException;

public synchronized static native int H5Pfill_value_defined(int plist_id,
        int[] status) throws HDF5LibraryException, NullPointerException;

/**
 * H5Pget retrieves a copy of the value for a property in a property list
 * (support integer only)
 * 
 * @param plid
 *            IN: Identifier of property object to query
 * @param name
 *            IN: Name of property to query
 * @return value for a property if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pget(int plid, String name)
        throws HDF5LibraryException;

/**
 * Sets a property list value (support integer only)
 * 
 * @param plid
 *            IN: Property list identifier to modify
 * @param name
 *            IN: Name of property to modify
 * @param value
 *            IN: value to set the property to
 * @return a non-negative value if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pset(int plid, String name,
        int value) throws HDF5LibraryException;

/**
 * H5Pget_alignment retrieves the current settings for alignment properties
 * from a file access property list.
 * 
 * @param plist
 *            IN: Identifier of a file access property list.
 * @param alignment
 *            OUT: threshold value and alignment value.
 * 
 *            <pre>
 *      alignment[0] = threshold // threshold value
 *      alignment[1] = alignment // alignment value
 * </pre>
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - aligment array is null.
 * @exception IllegalArgumentException
 *                - aligment array is invalid.
 **/
public synchronized static native int H5Pget_alignment(int plist,
        long[] alignment)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Pset_alignment sets the alignment properties of a file access property
 * list so that any file object >= THRESHOLD bytes will be aligned on an
 * address which is a multiple of ALIGNMENT.
 * 
 * @param plist
 *            IN: Identifier for a file access property list.
 * @param threshold
 *            IN: Threshold value.
 * @param alignment
 *            IN: Alignment value.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_alignment(int plist,
        long threshold, long alignment) throws HDF5LibraryException;

public synchronized static native int H5Pget_alloc_time(int plist_id,
        int[] alloc_time) throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_alloc_time(int plist_id,
        int alloc_time) throws HDF5LibraryException, NullPointerException;

/**
* H5Pget_attr_creation_order retrieves the settings for tracking and indexing attribute creation order on an object  
* @param ocpl_id            IN: Object (group or dataset) creation property list identifier
* 
* @return Flags specifying whether to track and index attribute creation order 
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pget_attr_creation_order(int ocpl_id)
        throws HDF5LibraryException;

/**
* H5Pset_attr_creation_order sets flags specifying whether to track and index attribute creation order on an object.  
* @param ocpl_id                  IN: Object creation property list identifier
* @param crt_order_flags          IN: Flags specifying whether to track and index attribute creation order
* 
* @return Returns a non-negative value if successful; otherwise returns a negative value. 
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pset_attr_creation_order(int ocpl_id, int crt_order_flags)
        throws HDF5LibraryException;

/**
* H5Pget_attr_phase_change retrieves attribute storage phase change thresholds. 
* @param ocpl_id      IN: : Object (dataset or group) creation property list identifier 
* @param attributes 
*               The maximun and minimum no. of attributes
*               to be stored.
*
*      <pre>
*      attributes[0] =  The maximum number of attributes to be stored in compact storage
*      attributes[1] =  The minimum number of attributes to be stored in dense storage 
*      </pre>
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - size is null.
*  
**/
public synchronized static native int H5Pget_attr_phase_change(int ocpl_id, int []attributes) 
        throws HDF5LibraryException, NullPointerException;

//herr_t H5Pset_attr_phase_change( hid_t ocpl_id, unsigned max_compact, unsigned min_dense ) 

/**
 * H5Pget_btree_ratio Get the B-tree split ratios for a dataset transfer
 * property list.
 * 
 * @param plist_id
 *            IN Dataset transfer property list
 * @param left
 *            OUT split ratio for leftmost nodes
 * @param right
 *            OUT split ratio for righttmost nodes
 * @param middle
 *            OUT split ratio for all other nodes
 * 
 * @return non-negative if succeed
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - an input array is null.
 **/
public synchronized static native int H5Pget_btree_ratios(int plist_id,
        double[] left, double[] middle, double[] right)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pset_btree_ratio Sets B-tree split ratios for a dataset transfer
 * property list. The split ratios determine what percent of children go in
 * the first node when a node splits.
 * 
 * @param plist_id
 *            IN Dataset transfer property list
 * @param left
 *            IN split ratio for leftmost nodes
 * @param right
 *            IN split ratio for righttmost nodes
 * @param middle
 *            IN split ratio for all other nodes
 * 
 * @return non-negative if succeed
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_btree_ratios(int plist_id,
        double left, double middle, double right)
        throws HDF5LibraryException;

/**
 * HH5Pget_buffer gets type conversion and background buffers. Returns
 * buffer size, in bytes, if successful; otherwise 0 on failure.
 * 
 * @param plist
 *            Identifier for the dataset transfer property list.
 * @param tconv
 *            byte array of application-allocated type conversion buffer.
 * @param bkg
 *            byte array of application-allocated background buffer.
 * 
 * @return buffer size, in bytes, if successful; otherwise 0 on failure
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception IllegalArgumentException
 *                - plist is invalid.
 **/
public synchronized static native int H5Pget_buffer(int plist,
        byte[] tconv, byte[] bkg)
        throws HDF5LibraryException, IllegalArgumentException;
public synchronized static native long H5Pget_buffer_size(int plist)
        throws HDF5LibraryException, IllegalArgumentException;

/**
 * H5Pset_buffer sets type conversion and background buffers. status to TRUE
 * or FALSE.
 * 
 * Given a dataset transfer property list, H5Pset_buffer sets the maximum
 * size for the type conversion buffer and background buffer and optionally
 * supplies pointers to application-allocated buffers. If the buffer size is
 * smaller than the entire amount of data being transferred between the
 * application and the file, and a type conversion buffer or background
 * buffer is required, then strip mining will be used.
 * 
 * Note that there are minimum size requirements for the buffer. Strip
 * mining can only break the data up along the first dimension, so the
 * buffer must be large enough to accommodate a complete slice that
 * encompasses all of the remaining dimensions. For example, when strip
 * mining a 100x200x300 hyperslab of a simple data space, the buffer must be
 * large enough to hold 1x200x300 data elements. When strip mining a
 * 100x200x300x150 hyperslab of a simple data space, the buffer must be
 * large enough to hold 1x200x300x150 data elements.
 * 
 * If tconv and/or bkg are null pointers, then buffers will be allocated and
 * freed during the data transfer.
 * 
 * @param plist
 *            Identifier for the dataset transfer property list.
 * @param size
 *            Size, in bytes, of the type conversion and background buffers.
 * @param tconv
 *            byte array of application-allocated type conversion buffer.
 * @param bkg
 *            byte array of application-allocated background buffer.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception IllegalArgumentException
 *                - plist is invalid.
 **/
public synchronized static native void H5Pset_buffer_size(int plist, long size)
        throws HDF5LibraryException, IllegalArgumentException;

/**
 * Retrieves the maximum possible number of elements in the meta data cache
 * and the maximum possible number of bytes and the RDCC_W0 value in the raw
 * data chunk cache.
 * 
 * @param plist       IN: Identifier of the file access property list.
 * @param mdc_nelmts  IN/OUT: No longer used, will be ignored.
 * @param rdcc_nelmts IN/OUT: Number of elements (objects) in the raw data chunk cache.
 * @param rdcc_nbytes IN/OUT: Total size of the raw data chunk cache, in bytes.
 * @param rdcc_w0     IN/OUT: Preemption policy.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - an array is null.
 **/
public synchronized static native int H5Pget_cache(int plist,
        int[] mdc_nelmts, long[] rdcc_nelmts, long[] rdcc_nbytes,
        double[] rdcc_w0) throws HDF5LibraryException, NullPointerException;
/** 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Pget_cache(int, int[], long[], long[], double[]) }
 *             because of possible loss of precision
 **/ 
@Deprecated
public static int H5Pget_cache(int plist,
        int[] mdc_nelmts, int[] rdcc_nelmts, int[] rdcc_nbytes,
        double[] rdcc_w0) throws HDF5LibraryException, NullPointerException
{
    long[] rdcc_nelmts_l = {rdcc_nelmts[0]};
    long[] rdcc_nbytes_l = {rdcc_nbytes[0]};
    int retval = H5Pget_cache(plist, mdc_nelmts, rdcc_nelmts_l, rdcc_nbytes_l, rdcc_w0);
    rdcc_nelmts[0] = (int)rdcc_nelmts_l[0];
    rdcc_nbytes[0] = (int)rdcc_nbytes_l[0];
    return retval;
}

/**
 * H5Pset_cache sets the number of elements (objects) in the meta data cache
 * and the total number of bytes in the raw data chunk cache.
 * 
 * @param plist       IN: Identifier of the file access property list.
 * @param mdc_nelmts  IN: No longer used, will be ignored.
 * @param rdcc_nelmts IN: Number of elements (objects) in the raw data chunk cache.
 * @param rdcc_nbytes IN: Total size of the raw data chunk cache, in bytes.
 * @param rdcc_w0     IN: Preemption policy.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_cache(int plist,
        int mdc_nelmts, long rdcc_nelmts, long rdcc_nbytes, double rdcc_w0)
        throws HDF5LibraryException;

public synchronized static native int H5Pget_char_encoding(int plist_id)
        throws HDF5LibraryException;
public synchronized static native void H5Pset_char_encoding(int plist_id, int encoding) 
        throws HDF5LibraryException;

/**
 * H5Pget_chunk retrieves the size of chunks for the raw data of a chunked
 * layout dataset.
 * 
 * @param plist
 *            IN: Identifier of property list to query.
 * @param max_ndims
 *            IN: Size of the dims array.
 * @param dims
 *            OUT: Array to store the chunk dimensions.
 * 
 * @return chunk dimensionality successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - dims array is null.
 * @exception IllegalArgumentException
 *                - max_ndims <=0
 **/
public synchronized static native int H5Pget_chunk(int plist,
        int max_ndims, long[] dims)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Pset_chunk sets the size of the chunks used to store a chunked layout
 * dataset.
 * 
 * @param plist
 *            IN: Identifier for property list to query.
 * @param ndims
 *            IN: The number of dimensions of each chunk.
 * @param dim
 *            IN: An array containing the size of each chunk.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - dims array is null.
 * @exception IllegalArgumentException
 *                - dims <=0
 **/
public synchronized static native int H5Pset_chunk(int plist, int ndims,
        byte[] dim)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

public synchronized static int H5Pset_chunk(int plist, int ndims, long[] dim)
        throws HDF5Exception, NullPointerException,
        IllegalArgumentException
{
    if (dim == null) {
        return -1;
    }

    HDFArray theArray = new HDFArray(dim);
    byte[] thedims = theArray.byteify();

    int retVal = H5Pset_chunk(plist, ndims, thedims);

    thedims = null;
    theArray = null;
    return retVal;
}


/**
 * Retrieves the maximum possible number of elements in the meta data cache
 * and the maximum possible number of bytes and the RDCC_W0 value in the raw
 * data chunk cache on a per-datset basis.
 * 
 * @param dapl_id     IN: Identifier of the dataset access property list.
 * @param rdcc_nslots IN/OUT: Number of elements (objects) in the raw data chunk cache.
 * @param rdcc_nbytes IN/OUT: Total size of the raw data chunk cache, in bytes.
 * @param rdcc_w0     IN/OUT: Preemption policy.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - an array is null.
 **/
public synchronized static native void H5Pget_chunk_cache(int dapl_id,
        long[] rdcc_nslots, long[] rdcc_nbytes, double[] rdcc_w0)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pset_chunk_cache sets the number of elements (objects) in the meta data cache
 * and the total number of bytes in the raw data chunk cache on a per-datset basis.
 * 
 * @param dapl_id     IN: Identifier of the datset access property list.
 * @param rdcc_nslots IN: Number of elements (objects) in the raw data chunk cache.
 * @param rdcc_nbytes IN: Total size of the raw data chunk cache, in bytes.
 * @param rdcc_w0     IN: Preemption policy.
 * 
 * @return none
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Pset_chunk_cache(int dapl_id,
        long rdcc_nslots, long rdcc_nbytes, double rdcc_w0)
        throws HDF5LibraryException;

/**
 * H5Pget_class returns the property list class for the property list
 * identified by the plist parameter.
 * 
 * @param plist
 *            IN: Identifier of property list to query.
 * @return a property list class if successful. Otherwise returns
 *         H5P_NO_CLASS (-1).
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pget_class(int plist)
        throws HDF5LibraryException;

/**
 * H5Pget_class_name retrieves the name of a generic property list class
 * 
 * @param plid
 *            IN: Identifier of property object to query
 * @return name of a property list if successful; null if failed
 * @throws HDF5LibraryException
 */
public synchronized static native String H5Pget_class_name(int plid)
        throws HDF5LibraryException;

/**
 * H5Pget_class_parent retrieves an identifier for the parent class of a
 * property class
 * 
 * @param plid
 *            IN: Identifier of the property class to query
 * @return a valid parent class object identifier if successful; a negative
 *         value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pget_class_parent(int plid)
        throws HDF5LibraryException;

/**
* H5Pget_copy_object retrieves the properties to be used when an object is copied.  
* @param ocp_plist_id            IN: Object copy property list identifier
*  
* @return Copy option(s) set in the object copy property list  
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pget_copy_object(int ocp_plist_id)
        throws HDF5LibraryException;

/**
* H5Pset_copy_object Sets properties to be used when an object is copied.  
* @param ocp_plist_id            IN: Object copy property list identifier
* @param copy_options          IN: Copy option(s) to be set
*  
* @return none 
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native void H5Pset_copy_object(int ocp_plist_id, int copy_options)
        throws HDF5LibraryException;

/**
* H5Pget_create_intermediate_group determines whether property is set to enable creating missing intermediate groups.  
* @param lcpl_id                IN:  Link creation property list identifier
*  
* @return Boolean true or false   
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native boolean H5Pget_create_intermediate_group(int lcpl_id)
        throws HDF5LibraryException;

/**
* H5Pset_create_intermediate_group specifies in property list whether to create missing intermediate groups 
* @param lcpl_id                IN: Link creation property list identifier
* @param crt_intermed_group    IN: Flag specifying whether to create intermediate groups upon the creation of an object 
*  
* @return a non-negative valule if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pset_create_intermediate_group(int lcpl_id, boolean crt_intermed_group)
        throws HDF5LibraryException;

/**
* H5Pget_data_transform retrieves the data transform expression previously set in the dataset transfer property list plist_id by H5Pset_data_transform.
* @param plist_id                IN: Identifier of the property list or class
* @param size                    IN: Number of bytes of the transform expression to copy to
* @param expression            OUT: A data transform expression
* 
* @return The size of the transform expression if successful; 0(zero) if no transform expression exists. Otherwise returns a negative value. 
* 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Size is <= 0.
*  
**/
public synchronized static native long H5Pget_data_transform( int plist_id, String[] expression, long size)
        throws HDF5LibraryException, IllegalArgumentException;

/**
* H5Pset_data_transform sets a data transform expression 
* @param plist_id                IN: Identifier of the property list or class
* @param expression            IN: Pointer to the null-terminated data transform expression 
*  
* @return a non-negative valule if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - expression is null.
*  
**/
public synchronized static native int H5Pset_data_transform(int plist_id, String expression)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pget_driver returns the identifier of the low-level file driver 
 * associated with the file access property list or data transfer 
 * property list plid.
 * 
 * @param plid
 *            IN: File access or data transfer property list identifier.
 * @return a valid low-level driver identifier if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pget_driver(int plid)
        throws HDF5LibraryException;

//herr_t H5Pset_driver( hid_t plist_id, hid_t new_driver_id, const void *new_driver_info ) 
//void *H5Pget_driver_info( hid_t plist_id ) 

//herr_t H5Pget_dxpl_mpio( hid_t dxpl_id, H5FD_mpio_xfer_t *xfer_mode ) 
//herr_t H5Pset_dxpl_mpio( hid_t dxpl_id, H5FD_mpio_xfer_t xfer_mode ) 
//herr_t H5Pset_dxpl_mpio_chunk_opt (hid_t dxpl_id, H5FD_mpio_chunk_opt_t opt_mode) 
//herr_t H5Pset_dxpl_mpio_chunk_opt_num (hid_t dxpl_id, unsigned num_chunk_per_proc) 
//herr_t H5Pset_dxpl_mpio_chunk_opt_ratio (hid_t dxpl_id, unsigned percent_proc_per_chunk) 
//herr_t H5Pset_dxpl_mpio_collective_opt (hid_t dxpl_id, H5FD_mpio_collective_opt_t opt_mode) 

public synchronized static native void H5Pget_dxpl_multi(int dxpl_id, int[] memb_dxpl) 
        throws HDF5LibraryException, NullPointerException;

public synchronized static native void H5Pset_dxpl_multi(int dxpl_id, int[] memb_dxpl) 
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pget_edc_check(int plist)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_edc_check(int plist, int check)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pget_elink_acc_flags retrieves the external link traversal file access flag from the specified link access property list.
* @param lapl_id                IN: Link access property list identifier 
* 
* @return File access flag for link traversal.  
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pget_elink_acc_flags(int lapl_id)
        throws HDF5LibraryException;

/**
* H5Pset_elink_acc_flags Sets the external link traversal file access flag in a link access property list. 
* @param lapl_id                    IN: Link access property list identifier
* @param flags                     IN: The access flag for external link traversal.  
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception IllegalArgumentException - Invalid Flag values.
*  
**/
public synchronized static native int H5Pset_elink_acc_flags(int lapl_id, int flags)
        throws HDF5LibraryException, IllegalArgumentException;

//herr_t H5Pget_elink_cb( hid_t lapl_id, H5L_elink_traverse_t *func, void **op_data ) 
//herr_t H5Pset_elink_cb( hid_t lapl_id, H5L_elink_traverse_t func, void *op_data ) 

/**
* H5Pget_elink_fapl Retrieves the file access property list identifier associated with 
* the link access property list.   
* 
* @param lapl_id                IN: Link access property list identifier
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public static int H5Pget_elink_fapl(int lapl_id)
        throws HDF5LibraryException
{
    int id = _H5Pget_elink_fapl(lapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Pget_elink_fapl(int lapl_id)
        throws HDF5LibraryException;

/**
 * H5Pset_elink_fapl sets a file access property list for use in accessing a 
 * file pointed to by an external link.  
 * 
 * @param lapl_id                IN: Link access property list identifier
 * @param fapl_id                IN: File access property list identifier
 *  
 * @return a non-negative value if successful; otherwise returns a negative value.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  
 **/
public synchronized static native int H5Pset_elink_fapl(int lapl_id, int fapl_id)
        throws HDF5LibraryException;

/**
 * H5Pget_elink_file_cache_size retrieves the size of the external link open file cache. 
 * @param fapl_id                 IN: File access property list identifier
 *  
 * @return External link open file cache size in number of files. 
 *  
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  
 **/
public synchronized static native int H5Pget_elink_file_cache_size(int fapl_id)
        throws HDF5LibraryException; 

/**
 * H5Pset_elink_file_cache_size sets the number of files that can be held open in an external link open file cache. 
 * @param fapl_id                 IN: File access property list identifier
 * @param efc_size                IN: External link open file cache size in number of files. 
 *  
 * @return none.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  
 **/
public synchronized static native void H5Pset_elink_file_cache_size(int fapl_id, int efc_size) 
        throws HDF5LibraryException;

/**
* H5Pget_elink_prefix Retrieves prefix applied to external link paths.
* @param lapl_id                IN: Link access property list identifier
* @param prefix                OUT: Prefix applied to external link paths
* 
* @return If successful, returns a non-negative value specifying the size in bytes of the prefix without the NULL terminator; 
*         otherwise returns a negative value.  
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - prefix is null.
*  
**/
public synchronized static native long H5Pget_elink_prefix(int lapl_id, String[] prefix)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pset_elink_prefix Sets prefix to be applied to external link paths.   
* @param lapl_id                IN: Link access property list identifier
* @param prefix                     IN: Prefix to be applied to external link paths
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - prefix is null.
*  
**/
public synchronized static native int H5Pset_elink_prefix(int lapl_id, String prefix)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pget_est_link_info Queries data required to estimate required local heap or object header size. 
* @param gcpl_id                IN: Group creation property list identifier 
* @param link_info
*               Estimated number of links to be inserted into group
*               And the estimated average length of link names         
*
*      <pre>
*      link_info[0] =  Estimated number of links to be inserted into group
*      link_info[1] =  Estimated average length of link names   
*      </pre>
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - link_info is null.
*  
**/
public synchronized static native int H5Pget_est_link_info(int gcpl_id, int []link_info) 
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pset_est_link_info Sets estimated number of links and length of link names in a group.  
* @param gcpl_id                IN: Group creation property list identifier
* @param est_num_entries         IN: Estimated number of links to be inserted into group
* @param est_name_len            IN: Estimated average length of link names
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Invalid values to est_num_entries and est_name_len.
*  
**/
public synchronized static native int H5Pset_est_link_info(int gcpl_id, int est_num_entries, int est_name_len)
        throws HDF5LibraryException, IllegalArgumentException;

/**
 * H5Pget_external returns information about an external file.
 * 
 * @param plist
 *            IN: Identifier of a dataset creation property list.
 * @param idx
 *            IN: External file index.
 * @param name_size
 *            IN: Maximum length of name array.
 * @param name
 *            OUT: Name of the external file.
 * @param size
 *            OUT: the offset value and the size of the external file data.
 * 
 *            <pre>
 *      size[0] = offset // a location to return an offset value
 *      size[1] = size // a location to return the size of
 *                // the external file data.
 * </pre>
 * 
 * @return a non-negative value if successful
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                Fatal error on Copyback
 * @exception ArrayStoreException
 *                Fatal error on Copyback
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name or size is null.
 * @exception IllegalArgumentException
 *                - name_size <= 0 .
 * 
 **/
public synchronized static native int H5Pget_external(int plist, int idx,
        long name_size, String[] name, long[] size)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException,
        HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Pset_external adds an external file to the list of external files.
 * 
 * @param plist
 *            IN: Identifier of a dataset creation property list.
 * @param name
 *            IN: Name of an external file.
 * @param offset
 *            IN: Offset, in bytes, from the beginning of the file to the
 *            location in the file where the data starts.
 * @param size
 *            IN: Number of bytes reserved in the file for the data.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - name is null.
 **/
public synchronized static native int H5Pset_external(int plist,
        String name, long offset, long size)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pget_external_count returns the number of external files for the
 * specified dataset.
 * 
 * @param plist
 *            IN: Identifier of a dataset creation property list.
 * 
 * @return the number of external files if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pget_external_count(int plist)
        throws HDF5LibraryException;

public synchronized static native long H5Pget_family_offset(int fapl_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_family_offset(int fapl_id,
        long offset) throws HDF5LibraryException, NullPointerException;

public synchronized static native void H5Pget_fapl_core(int fapl_id,
        long[] increment, boolean[] backing_store)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fapl_core(int fapl_id,
        long increment, boolean backing_store)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pget_fapl_direct   Retrieve direct I/O settings.
* @param fapl_id        IN: File access property list identifier 
* @param info[0] = alignment        OUT: Required memory alignment boundary 
* @param info[1] = block_size        OUT: File system block size 
* @param info[2] = cbuf_size        OUT: Copy buffer size 
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pget_fapl_direct(int fapl_id, long[]info) throws HDF5LibraryException;

/**
* H5Pset_fapl_direct Sets up use of the direct I/O driver.   
* @param fapl_id        IN: File access property list identifier 
* @param alignment        IN: Required memory alignment boundary 
* @param block_size        IN: File system block size 
* @param cbuf_size        IN: Copy buffer size 
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pset_fapl_direct(int fapl_id, long alignment, long block_size, long cbuf_size)
        throws HDF5LibraryException;

public synchronized static native int H5Pget_fapl_family(int fapl_id,
        long[] memb_size, int[] memb_fapl_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fapl_family(int fapl_id,
        long memb_size, int memb_fapl_id)
        throws HDF5LibraryException, NullPointerException;

//herr_t H5Pget_fapl_mpio( int fapl_id, MPI_Comm *comm, MPI_Info *info ) 
//herr_t H5Pset_fapl_mpio( int fapl_id, MPI_Comm comm, MPI_Info info ) 

//herr_t H5Pget_fapl_mpiposix( int fapl_id, MPI_Comm *comm, hbool_t *use_gpfs_hints ) 
//herr_t H5Pset_fapl_mpiposix( int fapl_id, MPI_Comm comm, hbool_t use_gpfs_hints ) 


/**
 * H5Pget_fapl_multi Sets up use of the multi I/O driver.   
 * @param fapl_id     IN: File access property list identifier 
 * @param memb_map    IN: Maps memory usage types to other memory usage types.
 * @param memb_fapl   IN: Property list for each memory usage type.
 * @param memb_name   IN: Name generator for names of member files.
 * @param memb_addr   IN: The offsets within the virtual address space, from 0 (zero) to HADDR_MAX, at which each type of data storage begins.
 *  
 * @return a boolean value; Allows read-only access to incomplete file sets when TRUE.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - an array is null.
 *  
 **/
public synchronized static native boolean H5Pget_fapl_multi(int fapl_id, int[] memb_map, 
        int[] memb_fapl, String[] memb_name, long[] memb_addr) 
        throws HDF5LibraryException, NullPointerException;


/**
 * H5Pset_fapl_multi Sets up use of the multi I/O driver.   
 * @param fapl_id     IN: File access property list identifier 
 * @param memb_map    IN: Maps memory usage types to other memory usage types.
 * @param memb_fapl   IN: Property list for each memory usage type.
 * @param memb_name   IN: Name generator for names of member files.
 * @param memb_addr   IN: The offsets within the virtual address space, from 0 (zero) to HADDR_MAX, at which each type of data storage begins.
 * @param relax       IN: Allows read-only access to incomplete file sets when TRUE.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - an array is null.
 *  
 **/
public synchronized static native void H5Pset_fapl_multi(int fapl_id, int[] memb_map, 
        int[] memb_fapl, String[] memb_name, long[] memb_addr, boolean relax) 
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pget_fclose_degree(int plist_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fclose_degree(int plist,
        int degree) throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pget_fill_time(int plist_id,
        int[] fill_time) throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fill_time(int plist_id,
        int fill_time) throws HDF5LibraryException, NullPointerException;

/**
 * H5Pget_fill_value queries the fill value property of a dataset creation
 * property list.
 * 
 * @param plist_id
 *            IN: Property list identifier.
 * @param type_id
 *            IN: The datatype identifier of value.
 * @param value
 *            IN: The fill value.
 * 
 * @return a non-negative value if successful
 * 
 **/
public synchronized static native int H5Pget_fill_value(int plist_id,
        int type_id, byte[] value) throws HDF5Exception;

/**
 * H5Pget_fill_value queries the fill value property of a dataset creation
 * property list.
 * 
 * @param plist_id
 *            IN: Property list identifier.
 * @param type_id
 *            IN: The datatype identifier of value.
 * @param obj
 *            IN: The fill value.
 * 
 * @return a non-negative value if successful
 * 
 **/
public synchronized static int H5Pget_fill_value(int plist_id, int type_id,
        Object obj) throws HDF5Exception
{
    HDFArray theArray = new HDFArray(obj);
    byte[] buf = theArray.emptyBytes();

    int status = H5Pget_fill_value(plist_id, type_id, buf);
    if (status >= 0) {
        obj = theArray.arrayify(buf);
    }

    return status;
}

/**
 * H5Pset_fill_value sets the fill value for a dataset creation property
 * list.
 * 
 * @param plist_id
 *            IN: Property list identifier.
 * @param type_id
 *            IN: The datatype identifier of value.
 * @param value
 *            IN: The fill value.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5Exception
 *                - Error converting data array
 **/
public synchronized static native int H5Pset_fill_value(int plist_id,
        int type_id, byte[] value) throws HDF5Exception;

/**
 * H5Pset_fill_value sets the fill value for a dataset creation property
 * list.
 * 
 * @param plist_id
 *            IN: Property list identifier.
 * @param type_id
 *            IN: The datatype identifier of value.
 * @param obj
 *            IN: The fill value.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5Exception
 *                - Error converting data array
 **/
public synchronized static int H5Pset_fill_value(int plist_id, int type_id,
        Object obj) throws HDF5Exception
{
    HDFArray theArray = new HDFArray(obj);
    byte[] buf = theArray.byteify();

    int retVal = H5Pset_fill_value(plist_id, type_id, buf);

    buf = null;
    theArray = null;
    return retVal;
}

/**
 * H5Pget_filter returns information about a filter, specified by its filter
 * number, in a filter pipeline, specified by the property list with which
 * it is associated.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Pget_filter(int, int, int[], int[], int[], int, String[], int[]) }
 * 
 * @param plist
 *            IN: Property list identifier.
 * @param filter_number
 *            IN: Sequence number within the filter pipeline of the filter
 *            for which information is sought.
 * @param flags
 *            OUT: Bit vector specifying certain general properties of the
 *            filter.
 * @param cd_nelmts
 *            IN/OUT: Number of elements in cd_values
 * @param cd_values
 *            OUT: Auxiliary data for the filter.
 * @param namelen
 *            IN: Anticipated number of characters in name.
 * @param name
 *            OUT: Name of the filter.
 * 
 * @return the filter identification number if successful. Otherwise returns
 *         H5Z_FILTER_ERROR (-1).
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                Fatal error on Copyback
 * @exception ArrayStoreException
 *                Fatal error on Copyback
 * @exception NullPointerException
 *                - name or an array is null.
 * 
 **/
@Deprecated
public synchronized static native int H5Pget_filter(int plist, int filter_number, int[] flags, 
        int[] cd_nelmts, int[] cd_values, int namelen, String[] name) 
        throws ArrayIndexOutOfBoundsException, ArrayStoreException, HDF5LibraryException, 
        NullPointerException;

/**
 * H5Pget_filter returns information about a filter, specified by its filter
 * number, in a filter pipeline, specified by the property list with which
 * it is associated.
 * 
 * @param plist            IN: Property list identifier.
 * @param filter_number    IN: Sequence number within the filter pipeline of the filter
 *                                 for which information is sought.
 * @param flags            OUT: Bit vector specifying certain general properties of the
 *                                 filter.
 * @param cd_nelmts        IN/OUT: Number of elements in cd_values
 * @param cd_values        OUT: Auxiliary data for the filter.
 * @param namelen            IN: Anticipated number of characters in name.
 * @param name                OUT: Name of the filter.
 * @param filter_config    OUT:A bit field encoding the returned filter information 
 * 
 * @return the filter identification number if successful. Otherwise returns
 *         H5Z_FILTER_ERROR (-1).
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                Fatal error on Copyback
 * @exception ArrayStoreException
 *                Fatal error on Copyback
 * @exception NullPointerException
 *                - name or an array is null.
 * 
 **/
public static int H5Pget_filter(int plist, int filter_number, int[] flags, long[] cd_nelmts, 
        int[] cd_values, long namelen, String[] name, int[] filter_config)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException, HDF5LibraryException, 
        NullPointerException
{
    return H5Pget_filter2(plist, filter_number, flags, cd_nelmts, cd_values, namelen, 
            name, filter_config);
}

/**
 * H5Pget_filter2 returns information about a filter, specified by its filter
 * number, in a filter pipeline, specified by the property list with which
 * it is associated.
 * 
 * @see public static int H5Pget_filter(int plist, int filter_number, int[] flags, 
 * int[] cd_nelmts, int[] cd_values, int namelen, String[] name, int[] filter_config)
 * 
 **/
private synchronized static native int H5Pget_filter2(int plist, int filter_number, int[] flags, 
        long[] cd_nelmts, int[] cd_values, long namelen, String[] name, int[] filter_config)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException, HDF5LibraryException, 
        NullPointerException;

/**
 * H5Pset_filter adds the specified filter and corresponding properties to
 * the end of an output filter pipeline.
 * 
 * @param plist
 *            IN: Property list identifier.
 * @param filter
 *            IN: Filter to be added to the pipeline.
 * @param flags
 *            IN: Bit vector specifying certain general properties of the
 *            filter.
 * @param cd_nelmts
 *            IN: Number of elements in cd_values
 * @param cd_values
 *            IN: Auxiliary data for the filter.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_filter(int plist, int filter,
        int flags, long cd_nelmts, int[] cd_values)
        throws HDF5LibraryException;

//herr_t H5Pset_filter_callback(hid_t plist, H5Z_filter_func_t func, void *op_data) 

/**
 * H5Pget_filter_by_id returns information about the filter specified in filter_id, a 
 * filter identifier. plist_id must be a dataset or group creation property list and 
 * filter_id must be in the associated filter pipeline. The filter_id and flags parameters 
 * are used in the same manner as described in the discussion of H5Pset_filter. Aside from 
 * the fact that they are used for output, the parameters cd_nelmts and cd_values[] are 
 * used in the same manner as described in the discussion of H5Pset_filter. On input, the 
 * cd_nelmts parameter indicates the number of entries in the cd_values[] array allocated 
 * by the calling program; on exit it contains the number of values defined by the filter.
 * On input, the namelen parameter indicates the number of characters allocated for the 
 * filter name by the calling program in the array name[]. On exit name[] contains the name 
 * of the filter with one character of the name in each element of the array. If the filter 
 * specified in filter_id is not set for the property list, an error will be returned and 
 * H5Pget_filter_by_id1 will fail.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Pget_filter_by_id(int, int, int[], int[], int[], int, String[], int[]) }
 * 
 * @param plist_id         IN: Property list identifier.
 * @param filter_id        IN: Filter identifier.
 * @param flags            OUT: Bit vector specifying certain general properties of the
 *                                 filter.
 * @param cd_nelmts        IN/OUT: Number of elements in cd_values
 * @param cd_values        OUT: Auxiliary data for the filter.
 * @param namelen          IN: Anticipated number of characters in name.
 * @param name             OUT: Name of the filter.
 * 
 * @return the filter identification number if successful. Otherwise returns
 *         H5Z_FILTER_ERROR (-1).
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                Fatal error on Copyback
 * @exception ArrayStoreException
 *                Fatal error on Copyback
 * @exception NullPointerException
 *                - name or an array is null.
 * 
 **/
@Deprecated
public synchronized static native int H5Pget_filter_by_id(int plist_id,
        int filter_id, int[] flags, long[] cd_nelmts, int[] cd_values,
        long namelen, String[] name)
        throws HDF5LibraryException, NullPointerException;
/**
 * H5Pget_filter_by_id returns information about the filter specified in filter_id, a 
 * filter identifier. plist_id must be a dataset or group creation property list and 
 * filter_id must be in the associated filter pipeline. The filter_id and flags parameters 
 * are used in the same manner as described in the discussion of H5Pset_filter. Aside from 
 * the fact that they are used for output, the parameters cd_nelmts and cd_values[] are 
 * used in the same manner as described in the discussion of H5Pset_filter. On input, the 
 * cd_nelmts parameter indicates the number of entries in the cd_values[] array allocated 
 * by the calling program; on exit it contains the number of values defined by the filter.
 * On input, the namelen parameter indicates the number of characters allocated for the 
 * filter name by the calling program in the array name[]. On exit name[] contains the name 
 * of the filter with one character of the name in each element of the array. If the filter 
 * specified in filter_id is not set for the property list, an error will be returned and 
 * H5Pget_filter_by_id1 will fail.
 * 
 * @param plist_id         IN: Property list identifier.
 * @param filter_id        IN: Filter identifier.
 * @param flags            OUT: Bit vector specifying certain general properties of the
 *                                 filter.
 * @param cd_nelmts        IN/OUT: Number of elements in cd_values
 * @param cd_values        OUT: Auxiliary data for the filter.
 * @param namelen          IN: Anticipated number of characters in name.
 * @param name             OUT: Name of the filter.
 * @param filter_config    OUT:A bit field encoding the returned filter information 
 * 
 * @return the filter identification number if successful. Otherwise returns
 *         H5Z_FILTER_ERROR (-1).
 * 
 * @exception ArrayIndexOutOfBoundsException
 *                Fatal error on Copyback
 * @exception ArrayStoreException
 *                Fatal error on Copyback
 * @exception NullPointerException
 *                - name or an array is null.
 * 
 **/
public static int H5Pget_filter_by_id(int plist_id,
        int filter_id, int[] flags, long[] cd_nelmts, int[] cd_values,
        long namelen, String[] name, int[] filter_config)
        throws ArrayIndexOutOfBoundsException, ArrayStoreException,
        HDF5LibraryException, NullPointerException
{
    return H5Pget_filter_by_id2(plist_id, filter_id, flags, cd_nelmts, cd_values,
            namelen, name, filter_config);
}
/**
 * H5Pget_filter_by_id2 returns information about a filter, specified by its filter
 * id, in a filter pipeline, specified by the property list with which
 * it is associated.
 * 
 * @see public static int H5Pget_filter_by_id(int plist,
        int filter_id, int[] flags, int[] cd_nelmts, int[] cd_values,
        int namelen, String[] name, int[] filter_config)
 **/
public synchronized static native int H5Pget_filter_by_id2(int plist_id,
        int filter_id, int[] flags, long[] cd_nelmts, int[] cd_values,
        long namelen, String[] name, int[] filter_config)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pget_gc_references Returns the current setting for the garbage
 * collection refernces property from a file access property list.
 * <p>
 * Note: this routine changed name with HDF5.1.2.2. If using an earlier
 * version, use 'configure --enable-hdf5_1_2_1' so this routine will link to
 * the old name.
 * 
 * @param fapl_id
 *            IN File access property list
 * @param gc_ref
 *            OUT GC is on (true) or off (false)
 * 
 * @return non-negative if succeed
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - array is null.
 **/
public synchronized static native int H5Pget_gc_references(int fapl_id,
        boolean[] gc_ref) throws HDF5LibraryException, NullPointerException;
public synchronized static native boolean H5Pget_gcreferences(int fapl_id)
        throws HDF5LibraryException;

/*
 * Earlier versions of the HDF5 library had a different name. This is
 * included as an alias.
 */
public synchronized static int H5Pget_gc_reference(int fapl_id,
        boolean[] gc_ref) throws HDF5LibraryException, NullPointerException
{
    return H5Pget_gc_references(fapl_id, gc_ref);
}

/**
 * H5Pset_gc_references Sets the flag for garbage collecting references for
 * the file. Default value for garbage collecting references is off.
 * 
 * @param fapl_id
 *            IN File access property list
 * @param gc_ref
 *            IN set GC on (true) or off (false)
 * 
 * @return non-negative if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_gc_references(int fapl_id,
        boolean gc_ref) throws HDF5LibraryException;

public synchronized static native int H5Pget_hyper_vector_size(int dxpl_id,
        long[] vector_size)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_hyper_vector_size(int dxpl_id,
        long vector_size) throws HDF5LibraryException, NullPointerException;

/**
 * H5Pget_istore_k queries the 1/2 rank of an indexed storage B-tree.
 * 
 * @param plist
 *            IN: Identifier of property list to query.
 * @param ik
 *            OUT: Pointer to location to return the chunked storage B-tree
 *            1/2 rank.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - ik array is null.
 **/
public synchronized static native int H5Pget_istore_k(int plist, int[] ik)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Pset_istore_k sets the size of the parameter used to control the
 * B-trees for indexing chunked datasets.
 * 
 * @param plist
 *            IN: Identifier of property list to query.
 * @param ik
 *            IN: 1/2 rank of chunked storage B-tree.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_istore_k(int plist, int ik)
        throws HDF5LibraryException;

/**
 * H5Pget_layout returns the layout of the raw data for a dataset.
 * 
 * @param plist
 *            IN: Identifier for property list to query.
 * 
 * @return the layout type of a dataset creation property list if
 *         successful. Otherwise returns H5D_LAYOUT_ERROR (-1).
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pget_layout(int plist)
        throws HDF5LibraryException;

/**
 * H5Pset_layout sets the type of storage used store the raw data for a
 * dataset.
 * 
 * @param plist
 *            IN: Identifier of property list to query.
 * @param layout
 *            IN: Type of storage layout for raw data.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_layout(int plist, int layout)
        throws HDF5LibraryException;

/**
* H5Pget_libver_bounds retrieves the lower and upper bounds on the HDF5 Library versions that indirectly determine the object formats versions used when creating objects in the file.
* @param fapl_id     IN: File access property list identifier
* @param libver 
*              The earliest/latest version of the library that will be used for writing objects.
*
*      <pre>
*      libver[0] =  The earliest version of the library that will be used for writing objects
*      libver[1] =  The latest version of the library that will be used for writing objects.
*      </pre>
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - size is null.
*  
**/
public synchronized static native int H5Pget_libver_bounds(int fapl_id, int []libver) 
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pset_libver_bounds Sets bounds on library versions, and indirectly format versions, to be used when creating objects
* @param fapl_id   IN: File access property list identifier
* @param low          IN: The earliest version of the library that will be used for writing objects
* @param high      IN: The latest version of the library that will be used for writing objects.
* 
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception IllegalArgumentException - Argument is Illegal
*  
**/
public synchronized static native int H5Pset_libver_bounds(int fapl_id, int low, int high)
        throws HDF5LibraryException, IllegalArgumentException;

/**
* H5Pget_link_creation_order queries the group creation property list, gcpl_id, and returns a flag indicating whether link creation order is tracked and/or indexed in a group.  
* @param gcpl_id      IN: Group creation property list identifier
* 
* @return crt_order_flags -Creation order flag(s)
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pget_link_creation_order(int gcpl_id)
        throws HDF5LibraryException;

/**
* H5Pset_link_creation_order Sets flags in a group creation property list, gcpl_id, for tracking and/or indexing links on creation order. 
* @param gcpl_id                  IN: Group creation property list identifier
* @param crt_order_flags          IN: Creation order flag(s)
* 
* 
* @return Returns a non-negative value if successful; otherwise returns a negative value. 
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pset_link_creation_order(int gcpl_id, int crt_order_flags)
        throws HDF5LibraryException;

/**
* H5Pget_link_phase_change Queries the settings for conversion between compact and dense groups.
* @param gcpl_id      IN: Group creation property list identifier
* @param links 
*               The max. no. of compact links & the min. no. of dense
*               links, which are used for storing groups
*
*      <pre>
*      links[0] =  The maximum number of links for compact storage
*      links[1] =  The minimum number of links for dense storage
*      </pre>
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - size is null.
*  
**/
public synchronized static native int H5Pget_link_phase_change(int gcpl_id, int []links) 
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pset_link_phase_change Sets the parameters for conversion between compact and dense groups. 
* @param gcpl_id                IN: Group creation property list identifier
* @param max_compact            IN: Maximum number of links for compact storage(Default: 8) 
* @param min_dense                IN: Minimum number of links for dense storage(Default: 6)
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception IllegalArgumentException - Invalid values of max_compact and min_dense.
*  
**/
public synchronized static native int H5Pset_link_phase_change(int gcpl_id, int max_compact, int min_dense)
        throws HDF5LibraryException, IllegalArgumentException;

/**
* H5Pget_local_heap_size_hint Retrieves the anticipated size of the local heap for original-style groups.  
* @param gcpl_id                IN: Group creation property list identifier
*  
* @return size_hint, the anticipated size of local heap
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native long H5Pget_local_heap_size_hint(int gcpl_id)
        throws HDF5LibraryException;

/**
* H5Pset_local_heap_size_hint Specifies the anticipated maximum size of a local heap. 
* @param gcpl_id              IN: Group creation property list identifier
* @param size_hint            IN: Anticipated maximum size in bytes of local heap
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pset_local_heap_size_hint(int gcpl_id, long size_hint)
        throws HDF5LibraryException;

/**
 * H5Pget_mdc_config gets the initial metadata cache configuration contained in a 
 * file access property list and loads it into the instance of H5AC_cache_config_t 
 * pointed to by the config_ptr parameter. This configuration is used when the file is opened. 
 * 
 * @param plist_id            IN: Identifier of the file access property list. 
 * 
 * @return  A buffer(H5AC_cache_config_t) for the current metadata cache configuration information 
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native H5AC_cache_config_t H5Pget_mdc_config(int plist_id) 
        throws HDF5LibraryException;
public synchronized static native void H5Pset_mdc_config(int plist_id, H5AC_cache_config_t config_ptr) 
        throws HDF5LibraryException; 

/**
 * H5Pget_meta_block_size the current metadata block size setting. 
 * @param fapl_id                 IN: File access property list identifier
 *  
 * @return the minimum size, in bytes, of metadata block allocations.
 *  
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  
 **/
public synchronized static native long H5Pget_meta_block_size(int fapl_id)
        throws HDF5LibraryException;

/**
 * H5Pset_meta_block_size sets the minimum metadata block size. 
 * @param fapl_id             IN: File access property list identifier
 * @param size                IN: Minimum size, in bytes, of metadata block allocations.
 *  
 * @return none.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  
 **/
public synchronized static native void H5Pset_meta_block_size(int fapl_id, long size)
        throws HDF5LibraryException;

//herr_t H5Pget_multi_type ( hid_t fapl_id, H5FD_mem_t *type ) 
//herr_t H5Pset_multi_type ( hid_t fapl_id, H5FD_mem_t type ) 

/**
 * H5Pget_nfilters returns the number of filters defined in the filter
 * pipeline associated with the property list plist.
 * 
 * @param plist
 *            IN: Property list identifier.
 * 
 * @return the number of filters in the pipeline if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pget_nfilters(int plist)
        throws HDF5LibraryException;

/**
* H5Pget_nlinks retrieves the maximum number of soft or user-defined link traversals allowed, nlinks, before the library assumes it has found a cycle and aborts the traversal. This value is retrieved from the link access property list lapl_id.
* @param lapl_id     IN: File access property list identifier
* 
* @return Returns a Maximum number of links to traverse. 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native long H5Pget_nlinks(int lapl_id)
        throws HDF5LibraryException;

/**
* H5Pset_nlinks sets the maximum number of soft or user-defined link traversals allowed, nlinks, before the library assumes it has found a cycle and aborts the traversal. This value is set in the link access property list lapl_id. 
* @param fapl_id     IN: File access property list identifier
* @param nlinks     IN: Maximum number of links to traverse
* 
* @return Returns a non-negative value if successful; otherwise returns a negative value. 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Argument is Illegal
*  
**/
public synchronized static native int H5Pset_nlinks(int lapl_id, long nlinks)
        throws HDF5LibraryException, IllegalArgumentException;

/**
 * H5Pget_nprops retrieves the number of properties in a property list or
 * class
 * 
 * @param plid
 *            IN: Identifier of property object to query
 * @return number of properties if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native long H5Pget_nprops(int plid)
        throws HDF5LibraryException;

/**
 * H5Pget_preserve checks the status of the dataset transfer property list.
 * 
 * @deprecated As of HDF5 1.8, compound datatype field preservation is now core functionality in the HDF5 Library.
 * 
 * @param plist
 *            IN: Identifier for the dataset transfer property list.
 * 
 * @return TRUE or FALSE if successful; otherwise returns a negative value
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
@Deprecated
public synchronized static native int H5Pget_preserve(int plist)
        throws HDF5LibraryException;

/**
 * H5Pset_preserve sets the dataset transfer property list status to TRUE or
 * FALSE.
 * 
 * @deprecated As of HDF5 1.8, compound datatype field preservation is now core functionality in the HDF5 Library.
 * 
 * @param plist
 *            IN: Identifier for the dataset transfer property list.
 * @param status
 *            IN: Status of for the dataset transfer property list.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception IllegalArgumentException
 *                - plist is invalid.
 **/
@Deprecated
public synchronized static native int H5Pset_preserve(int plist,
        boolean status)
        throws HDF5LibraryException, IllegalArgumentException;

/**
 * H5Pget_obj_track_times queries the object creation property list, ocpl_id, 
 * to determine whether object times are being recorded. 
 * 
 * @param ocpl_id   IN: Object creation property list identifier
 * 
 * @return TRUE or FALSE, specifying whether object times are being recorded
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * 
 **/
public synchronized static native boolean H5Pget_obj_track_times(int ocpl_id)
        throws HDF5LibraryException;

/**
 * H5Pset_obj_track_times sets a property in the object creation property list, ocpl_id, 
 * that governs the recording of times associated with an object. 
 * 
 * @param ocpl_id     IN: Object creation property list identifier
 * 
 * @param track_times IN: TRUE or FALSE, specifying whether object times are to be tracked 
 * 
 * @return none
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * 
 **/
public synchronized static native void H5Pset_obj_track_times(int ocpl_id, boolean track_times)
        throws HDF5LibraryException;

/**
* H5Pget_shared_mesg_index Retrieves the configuration settings for a shared message index.  
* @param fcpl_id            IN: File creation property list identifier 
* @param index_num        IN: Index being configured.
* @param mesg_info
*               The message type and minimum message size            
*
*      <pre>
*      mesg_info[0] =  Types of messages that may be stored in this index.
*      mesg_info[1] =  Minimum message size.  
*      </pre>
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - mesg_info is null.
* @exception IllegalArgumentException - Invalid value of nindexes
*  
**/
public synchronized static native int H5Pget_shared_mesg_index(int fcpl_id, int index_num,  int []mesg_info) 
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

/**
* H5Pset_shared_mesg_index Configures the specified shared object header message index 
* @param fcpl_id                IN: File creation property list identifier.
* @param index_num            IN: Index being configured.
* @param mesg_type_flags        IN: Types of messages that should be stored in this index.
* @param min_mesg_size          IN: Minimum message size.
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Invalid value of nindexes
*  
**/
public synchronized static native int H5Pset_shared_mesg_index(int fcpl_id, int index_num, int mesg_type_flags, int min_mesg_size)
        throws HDF5LibraryException, IllegalArgumentException;

/**
* H5Pget_shared_mesg_nindexes retrieves number of shared object header message indexes in file creation property list. 
* @param fcpl_id            IN: : File creation property list identifier 
*
* @return nindexes, the number of shared object header message indexes available in files created with this property list 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pget_shared_mesg_nindexes(int fcpl_id) 
        throws HDF5LibraryException;

/**
* H5Pset_shared_mesg_nindexes sets the number of shared object header message indexes in the specified file creation property list. 
* @param plist_id                IN: File creation property list 
* @param nindexes                  IN: Number of shared object header message indexes to be available in files created with this property list
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Invalid value of nindexes
*  
**/
public synchronized static native int H5Pset_shared_mesg_nindexes(int plist_id, int nindexes)
        throws HDF5LibraryException, IllegalArgumentException;

/**
* H5Pget_shared_mesg_phase_change retrieves shared object header message phase change information. 
* @param fcpl_id            IN: : File creation property list identifier 
* @param size
*               The threshold values for storage of shared object header 
*               message indexes in a file.
*
*      <pre>
*      size[0] =  Threshold above which storage of a shared object header message index shifts from list to B-tree 
*      size[1] =  Threshold below which storage of a shared object header message index reverts to list format  
*      </pre>
*      
* @return Returns a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - size is null.
*  
**/
public synchronized static native int H5Pget_shared_mesg_phase_change(int fcpl_id, int []size) 
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pset_shared_mesg_phase_change sets shared object header message storage phase change thresholds. 
* @param fcpl_id                IN: File creation property list identifier
* @param max_list                IN: Threshold above which storage of a shared object header message index shifts from list to B-tree
* @param min_btree                IN: Threshold below which storage of a shared object header message index reverts to list format
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Invalid values of max_list and min_btree.
*  
**/
public synchronized static native int H5Pset_shared_mesg_phase_change(int fcpl_id, int max_list, int min_btree)
        throws HDF5LibraryException, IllegalArgumentException;

public synchronized static native long H5Pget_sieve_buf_size(int fapl_id)
        throws HDF5LibraryException;
public synchronized static native void H5Pset_sieve_buf_size(int fapl_id, long size) 
        throws HDF5LibraryException;

/**
 * H5Pget_size retrieves the size of a property's value in bytes
 * 
 * @param plid
 *            IN: Identifier of property object to query
 * @param name
 *            IN: Name of property to query
 * @return size of a property's value if successful; a negative value if
 *         failed
 * @throws HDF5LibraryException
 */
public synchronized static native long H5Pget_size(int plid, String name)
        throws HDF5LibraryException;

/**
 * H5Pget_sizes retrieves the size of the offsets and lengths used in an
 * HDF5 file. This function is only valid for file creation property lists.
 * 
 * @param plist
 *            IN: Identifier of property list to query.
 * @param size
 *            OUT: the size of the offsets and length.
 * 
 *            <pre>
 *      size[0] = sizeof_addr // offset size in bytes
 *      size[1] = sizeof_size // length size in bytes
 * </pre>
 * @return a non-negative value with the sizes initialized; if successful;
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - size is null.
 * @exception IllegalArgumentException
 *                - size is invalid.
 **/
public synchronized static native int H5Pget_sizes(int plist, long[] size)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Pset_sizes sets the byte size of the offsets and lengths used to
 * address objects in an HDF5 file.
 * 
 * @param plist
 *            IN: Identifier of property list to modify.
 * @param sizeof_addr
 *            IN: Size of an object offset in bytes.
 * @param sizeof_size
 *            IN: Size of an object length in bytes.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_sizes(int plist,
        int sizeof_addr, int sizeof_size) throws HDF5LibraryException;

/**
 * H5Pget_small_data_block_size retrieves the size of a block of small data
 * in a file creation property list.
 * 
 * @param plist
 *            IN: Identifier for property list to query.
 * @param size
 *            OUT: Pointer to location to return block size.
 * 
 * @return a non-negative value and the size of the user block; if
 *         successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - size is null.
 **/
public synchronized static native int H5Pget_small_data_block_size(
        int plist, long[] size)
        throws HDF5LibraryException, NullPointerException;
public synchronized static native long H5Pget_small_data_block_size_long(
        int plist)
        throws HDF5LibraryException;

/**
 * H5Pset_small_data_block_size reserves blocks of size bytes for the
 * contiguous storage of the raw data portion of small datasets.
 * 
 * @param plist
 *            IN: Identifier of property list to modify.
 * @param size
 *            IN: Size of the blocks in bytes.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_small_data_block_size(
        int plist, long size) throws HDF5LibraryException;

/**
 * H5Pget_sym_k retrieves the size of the symbol table B-tree 1/2 rank and
 * the symbol table leaf node 1/2 size.
 * 
 * @param plist
 *            IN: Property list to query.
 * @param size
 *            OUT: the symbol table's B-tree 1/2 rank and leaf node 1/2
 *            size.
 * 
 *            <pre>
 *      size[0] = ik // the symbol table's B-tree 1/2 rank
 *      size[1] = lk // leaf node 1/2 size
 * </pre>
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - size is null.
 * @exception IllegalArgumentException
 *                - size is invalid.
 **/
public synchronized static native int H5Pget_sym_k(int plist, int[] size)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

/**
 * H5Pset_sym_k sets the size of parameters used to control the symbol table
 * nodes.
 * 
 * @param plist
 *            IN: Identifier for property list to query.
 * @param ik
 *            IN: Symbol table tree rank.
 * @param lk
 *            IN: Symbol table node size.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_sym_k(int plist, int ik, int lk)
        throws HDF5LibraryException;

//herr_t H5Pget_type_conv_cb(hid_t plist, H5T_conv_except_func_t *func, void **op_data) 
//herr_t H5Pset_type_conv_cb( hid_t plist, H5T_conv_except_func_t func, void *op_data) 

/**
 * H5Pget_userblock retrieves the size of a user block in a file creation
 * property list.
 * 
 * @param plist
 *            IN: Identifier for property list to query.
 * @param size
 *            OUT: Pointer to location to return user-block size.
 * 
 * @return a non-negative value and the size of the user block; if
 *         successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - size is null.
 **/
public synchronized static native int H5Pget_userblock(int plist,
        long[] size) throws HDF5LibraryException, NullPointerException;

/**
 * H5Pset_userblock sets the user block size of a file creation property
 * list.
 * 
 * @param plist
 *            IN: Identifier of property list to modify.
 * @param size
 *            IN: Size of the user-block in bytes.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_userblock(int plist, long size)
        throws HDF5LibraryException;

/**
 * H5Pget_version retrieves the version information of various objects for a
 * file creation property list.
 * 
 * @param plist
 *            IN: Identifier of the file creation property list.
 * @param version_info
 *            OUT: version information.
 * 
 *            <pre>
 *      version_info[0] = boot  // boot block version number
 *      version_info[1] = freelist  // global freelist version
 *      version_info[2] = stab  // symbol tabl version number
 *      version_info[3] = shhdr  // hared object header version
 * </pre>
 * @return a non-negative value, with the values of version_info
 *         initialized, if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - version_info is null.
 * @exception IllegalArgumentException
 *                - version_info is illegal.
 **/
public synchronized static native int H5Pget_version(int plist, int[] version_info)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

//herr_t H5Pget_vlen_mem_manager(hid_t plist, H5MM_allocate_t *alloc, void **alloc_info, H5MM_free_t *free, void **free_info ) 
//herr_t H5Pset_vlen_mem_manager(hid_t plist, H5MM_allocate_t alloc, void *alloc_info, H5MM_free_t free, void *free_info ) 

//herr_t H5Pinsert( hid_t plid, const char *name, size_t size, void *value, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_close_func_t close )
//herr_t H5Pinsert( hid_t plid, const char *name, size_t size, void *value, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_compare_func_t compare, H5P_prp_close_func_t close )    [2] 
//herr_t H5Pinsert1( hid_t plid, const char *name, size_t size, void *value, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_close_func_t close ) 
//herr_t H5Pinsert2( hid_t plid, const char *name, size_t size, void *value, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_compare_func_t compare, H5P_prp_close_func_t close ) 

/**
 * H5Pisa_class checks to determine whether a property list is a member of
 * the specified class
 * 
 * @param plist
 *            IN: Identifier of the property list
 * @param pclass
 *            IN: Identifier of the property class
 * @return a positive value if equal; zero if unequal; a negative value if
 *         failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Pisa_class(int plist, int pclass)
        throws HDF5LibraryException;

//int H5Piterate( hid_t id, int * idx, H5P_iterate_t iter_func, void * iter_data ) 

public synchronized static native int H5Pmodify_filter(int plist,
        int filter, int flags, long cd_nelmts, int[] cd_values)
        throws HDF5LibraryException, NullPointerException;

// herr_t H5Pregister( hid_t class, const char * name, size_t size, void * default, H5P_prp_create_func_t create, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_close_func_t close ) [1]
// herr_t H5Pregister( hid_t class, const char * name, size_t size, void * default, H5P_prp_create_func_t create, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_compare_func_t compare, H5P_prp_close_func_t close )     [2]
//herr_t H5Pregister1( hid_t class, const char * name, size_t size, void * default, H5P_prp_create_func_t create, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_close_func_t close ) 
//herr_t H5Pregister2( hid_t class, const char * name, size_t size, void * default, H5P_prp_create_func_t create, H5P_prp_set_func_t set, H5P_prp_get_func_t get, H5P_prp_delete_func_t delete, H5P_prp_copy_func_t copy, H5P_prp_compare_func_t compare, H5P_prp_close_func_t close )

/**
 * H5Punregister removes a property from a property list class
 * 
 * @param plid
 *            IN: Property list class from which to remove permanent
 *            property
 * @param name
 *            IN: Name of property to remove
 * @return a non-negative value if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Punregister(int plid, String name)
        throws HDF5LibraryException;

/**
 * H5Premove removes a property from a property list
 * 
 * @param plid
 *            IN: Identifier of the property list to modify
 * @param name
 *            IN: Name of property to remove
 * @return a non-negative value if successful; a negative value if failed
 * @throws HDF5LibraryException
 */
public synchronized static native int H5Premove(int plid, String name)
        throws HDF5LibraryException;

public synchronized static native int H5Premove_filter(int obj_id,
        int filter) throws HDF5LibraryException;

/**
 * H5Pset_deflate sets the compression method for a dataset.
 * 
 * @param plist
 *            IN: Identifier for the dataset creation property list.
 * @param level
 *            IN: Compression level.
 * 
 * @return non-negative if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Pset_deflate(int plist, int level)
        throws HDF5LibraryException;

/**
 *  H5Pset_fapl_log Sets up the logging virtual file driver (H5FD_LOG) for use.
 *  H5Pset_fapl_log modifies the file access property list to use the logging driver, H5FD_LOG. 
 *  The logging virtual file driver (VFD) is a clone of the standard SEC2 (H5FD_SEC2) driver 
 *  with additional facilities for logging VFD metrics and activity to a file. 
 *
 *  @deprecated As of HDF5 1.8.7, replaced by {@link #H5Pset_fapl_log(int, String, long, int)}
 *
 *  @param fapl_id  IN: File access property list identifier. 
 *  @param logfile  IN: logfile is the name of the file in which the logging entries are to be recorded.
 *  @param flags    IN: Flags specifying the types of logging activity.
 *  @param buf_size IN: The size of the logging buffers, in bytes.
 *
 *  @return a non-negative value if successful
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - logfile is null.
 **/
@Deprecated
public static int H5Pset_fapl_log(int fapl_id,
        String logfile, int flags, int buf_size)
        throws HDF5LibraryException, NullPointerException
{
    H5Pset_fapl_log(fapl_id, logfile, (long)flags, (long)buf_size);
    return 1;
}

/**
 *  H5Pset_fapl_log Sets up the logging virtual file driver (H5FD_LOG) for use.
 *  H5Pset_fapl_log modifies the file access property list to use the logging driver, H5FD_LOG. 
 *  The logging virtual file driver (VFD) is a clone of the standard SEC2 (H5FD_SEC2) driver 
 *  with additional facilities for logging VFD metrics and activity to a file. 
 *
 *  @param fapl_id  IN: File access property list identifier. 
 *  @param logfile  IN: logfile is the name of the file in which the logging entries are to be recorded.
 *  @param flags    IN: Flags specifying the types of logging activity.
 *  @param buf_size IN: The size of the logging buffers, in bytes.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - logfile is null.
 **/
public synchronized static native void H5Pset_fapl_log(int fapl_id,
        String logfile, long flags, long buf_size)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fapl_sec2(int fapl_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native void H5Pset_fapl_split(int fapl_id, 
        String meta_ext, int meta_plist_id, String raw_ext, int raw_plist_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fapl_stdio(int fapl_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fapl_windows(int fapl_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_fletcher32(int plist)
        throws HDF5LibraryException, NullPointerException;

/**
* H5Pset_nbit Sets up the use of the N-Bit filter.  
* @param plist_id                IN: Dataset creation property list identifier.
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
*  
**/
public synchronized static native int H5Pset_nbit(int plist_id)
        throws HDF5LibraryException;

/**
* H5Pset_scaleoffset sets the Scale-Offset filter for a dataset.   
* @param plist_id                IN: Dataset creation property list identifier.
* @param scale_type            IN: Flag indicating compression method.
* @param scale_factor            IN: Parameter related to scale.
*  
* @return a non-negative value if successful; otherwise returns a negative value.
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception IllegalArgumentException - Invalid arguments
*  
**/
public synchronized static native int H5Pset_scaleoffset(int plist_id, int scale_type, int scale_factor)
        throws HDF5LibraryException, IllegalArgumentException;

public synchronized static native int H5Pset_shuffle(int plist_id)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Pset_szip(int plist,
        int options_mask, int pixels_per_block)
        throws HDF5LibraryException, NullPointerException;

//////////////////////////////////////////////////////////////
////
//H5R: HDF5 1.8 Reference API Functions                     //
////
//////////////////////////////////////////////////////////////

private synchronized static native int H5Rcreate(byte[] ref, int loc_id,
        String name, int ref_type, int space_id)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

/**
* H5Rcreate creates the reference, ref, of the type specified in ref_type,
* pointing to the object name located at loc_id.
* 
* @param loc_id
*            IN: Location identifier used to locate the object being
*            pointed to.
* @param name
*            IN: Name of object at location loc_id.
* @param ref_type
*            IN: Type of reference.
* @param space_id
*            IN: Dataspace identifier with selection.
* 
* @return the reference (byte[]) if successful
* 
* @exception HDF5LibraryException
*                - Error from the HDF-5 Library.
* @exception NullPointerException
*                - an input array is null.
* @exception IllegalArgumentException
*                - an input array is invalid.
**/
public synchronized static byte[] H5Rcreate(int loc_id, String name,
        int ref_type, int space_id)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException
{
    /* These sizes are correct for HDF5.1.2 */
    int ref_size = 8;
    if (ref_type == HDF5Constants.H5R_DATASET_REGION) {
        ref_size = 12;
    }
    byte rbuf[] = new byte[ref_size];

    /* will raise an exception if fails */
    H5Rcreate(rbuf, loc_id, name, ref_type, space_id);

    return rbuf;
}

/**
 * Given a reference to some object, H5Rdereference opens that object and
 * return an identifier.
 * 
 * @param dataset
 *            IN: Dataset containing reference object.
 * @param ref_type
 *            IN: The reference type of ref.
 * @param ref
 *            IN: reference to an object
 * 
 * @return valid identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - output array is null.
 * @exception IllegalArgumentException
 *                - output array is invalid.
 **/
public static int H5Rdereference(int dataset, int ref_type, byte[] ref)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException
{
    int id = _H5Rdereference(dataset, ref_type, ref);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Rdereference(int dataset,
        int ref_type, byte[] ref)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
* H5Rget_name retrieves a name for the object identified by ref.
* @param loc_id     IN: Identifier for the dataset containing the reference or for the group that dataset is in.
* @param ref_type         IN: Type of reference.
* @param ref     IN: An object or dataset region reference.
* @param name     OUT: A name associated with the referenced object or dataset region.
* @param size     IN: The size of the name buffer.
* 
* @return Returns the length of the name if successful, returning 0 (zero) if no name is associated with the identifier. Otherwise returns a negative value. 
* 
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - size is null.
* @exception IllegalArgumentException - Argument is illegal.
*  
**/
public synchronized static native long H5Rget_name( int loc_id, int ref_type, byte[] ref, String[] name, long size)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

/*
 * [NOTE: This function is only supported in HDF5 Releases 1.4.x. It has
 * been replaced in Release 1.6 by the function H5Rget_obj_type public
 * synchronized static native int H5Rget_object_type(int loc_id, byte ref[])
 * throws HDF5LibraryException, NullPointerException,
 * IllegalArgumentException;
 */
/**
 * Given a reference to an object ref, H5Rget_obj_type returns the type of
 * the object pointed to.
 * 
 * @deprecated As of HDF5 1.8, replaced by {@link #H5Rget_obj_type(int, int, byte[], int[]) }
 * 
 * @param loc_id
 *            IN: loc_id of the reference object.
 * @param ref_type
 *            IN: Type of reference to query. *
 * @param ref
 *            IN: the reference
 * 
 * @return a valid object type if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - array is null.
 * @exception IllegalArgumentException
 *                - array is invalid.
 **/
@Deprecated
public synchronized static native int H5Rget_obj_type(int loc_id,
        int ref_type, byte ref[])
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Rget_obj_type Given a reference to an object ref, H5Rget_obj_type returns the type of
 * the object pointed to.
 * 
 * @param loc_id        IN: loc_id of the reference object.
 * @param ref_type        IN: Type of reference to query. 
 * @param ref            IN: the reference
 * @param obj_type        OUT:Type of referenced object
 * 
 * @return Returns the object type, which is the same as obj_type[0]. The return value is
 *         the same as the HDF5 1.6 version. 
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - array is null.
 * @exception IllegalArgumentException
 *                - array is invalid.
 **/
public static int H5Rget_obj_type(int loc_id,
        int ref_type, byte ref[], int [] obj_type)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException
{
    return H5Rget_obj_type2(loc_id, ref_type, ref, obj_type);
}

/**
 * H5Rget_obj_type2 Retrieves the type of object that an object reference points to. 
 * 
 * @see public static int H5Rget_obj_type(int loc_id, int ref_type, byte ref[], int [] obj_type)
 **/
private synchronized static native int H5Rget_obj_type2(int loc_id,
        int ref_type, byte ref[], int [] obj_type)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

/**
 * Given a reference to an object ref, H5Rget_region creates a copy of the
 * dataspace of the dataset pointed to and defines a selection in the copy
 * which is the region pointed to.
 * 
 * @param loc_id
 *            IN: loc_id of the reference object.
 * @param ref_type
 *            IN: The reference type of ref.
 * @param ref
 *            OUT: the reference to the object and region
 * 
 * @return a valid identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - output array is null.
 * @exception IllegalArgumentException
 *                - output array is invalid.
 **/
public static int H5Rget_region(int loc_id, int ref_type, byte[] ref)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException
{
    int id = _H5Rget_region(loc_id, ref_type, ref);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Rget_region(int loc_id,
        int ref_type, byte[] ref)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

//////////////////////////////////////////////////////////////
//                                                          //
//H5S: Dataspace Interface Functions                        //
//                                                          //
//////////////////////////////////////////////////////////////

/**
 * H5Sclose releases a dataspace.
 * 
 * @param space_id
 *            Identifier of dataspace to release.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Sclose(int space_id) throws HDF5LibraryException
{
    if (space_id < 0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(space_id);
    return _H5Sclose(space_id);
}

private synchronized static native int _H5Sclose(int space_id)
        throws HDF5LibraryException;

/**
 * H5Scopy creates a new dataspace which is an exact copy of the dataspace
 * identified by space_id.
 * 
 * @param space_id
 *            Identifier of dataspace to copy.
 * @return a dataspace identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Scopy(int space_id) throws HDF5LibraryException
{
    int id = _H5Scopy(space_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Scopy(int space_id)
        throws HDF5LibraryException;

/**
*  H5Screate creates a new dataspace of a particular type.
*
*  @param type IN: The type of dataspace to be created.

*  @return a dataspace identifier
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public static int H5Screate(int type) throws HDF5LibraryException
{
    int id = _H5Screate(type);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Screate(int type)
     throws HDF5LibraryException;

/**
 *  H5Screate_simple creates a new simple data space and opens
 *  it for access.
 *
 *  @param rank    IN: Number of dimensions of dataspace.
 *  @param dims    IN: An array of the size of each dimension.
 *  @param maxdims IN: An array of the maximum size of each dimension.
 *
 *  @return a dataspace identifier
 *
 *  @exception HDF5Exception - Error from the HDF-5 Library.
 *  @exception NullPointerException - dims or maxdims is null.
 **/
public static int H5Screate_simple(int rank, long[] dims, long[] maxdims)
throws HDF5Exception, NullPointerException
{
    int id = _H5Screate_simple(rank, dims, maxdims);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Screate_simple(int rank, long[] dims,
        long[] maxdims) throws HDF5Exception, NullPointerException;

/**
 *  @deprecated use H5Screate_simple(int rank, long[] dims, long[] maxdims)
 **/
@Deprecated
public static int H5Screate_simple(int rank, byte[] dims, byte[] maxdims)
        throws HDF5Exception, NullPointerException
{
    ByteBuffer dimsbb = ByteBuffer.wrap(dims);
    long[] ladims = (dimsbb.asLongBuffer()).array();
    ByteBuffer maxdimsbb = ByteBuffer.wrap(maxdims);
    long[] lamaxdims = (maxdimsbb.asLongBuffer()).array();

    int id = _H5Screate_simple(rank, ladims, lamaxdims);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

/**
 *  H5Sdecode reconstructs the HDF5 data space object and returns a 
 *  new object handle for it.
 *
 *  @param buf   IN: Buffer for the data space object to be decoded.
 *
 *  @return a new object handle
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native int H5Sdecode(byte[] buf)
throws HDF5LibraryException, NullPointerException;

/**
 *  H5Sencode converts a data space description into binary form in a buffer.
 *
 *  @param obj_id   IN: Identifier of the object to be encoded.
 *
 *  @return the buffer for the object to be encoded into.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native byte[] H5Sencode(int obj_id)
throws HDF5LibraryException, NullPointerException;

/**
 * H5Sextent_copy copies the extent from source_space_id to dest_space_id.
 * This action may change the type of the dataspace.
 * 
 * @param dest_space_id
 *            IN: The identifier for the dataspace from which the extent is
 *            copied.
 * @param source_space_id
 *            IN: The identifier for the dataspace to which the extent is
 *            copied.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sextent_copy(int dest_space_id,
        int source_space_id) throws HDF5LibraryException;

/**
 * H5Sextent_equal determines whether the dataspace extents of two dataspaces, 
 * space1_id and space2_id, are equal. 
 * 
 * @param first_space_id
 *            IN: The identifier for the first dataspace.
 * @param second_space_id
 *            IN: The identifier for the seconddataspace.
 * 
 * @return true if successful, else false
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native boolean H5Sextent_equal(int first_space_id,
        int second_space_id) throws HDF5LibraryException;

/**
 * H5Sget_select_bounds retrieves the coordinates of the bounding box
 * containing the current selection and places them into user-supplied
 * buffers.
 * <P>
 * The start and end buffers must be large enough to hold the dataspace rank
 * number of coordinates.
 * 
 * @param spaceid
 *            Identifier of dataspace to release.
 * @param start
 *            coordinates of lowest corner of bounding box.
 * @param end
 *            coordinates of highest corner of bounding box.
 * 
 * @return a non-negative value if successful,with start and end
 *         initialized.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - start or end is null.
 **/
public synchronized static native int H5Sget_select_bounds(int spaceid,
        long[] start, long[] end)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Sget_select_elem_npoints returns the number of element points in the
 * current dataspace selection.
 * 
 * @param spaceid
 *            Identifier of dataspace to release.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Sget_select_elem_npoints(
        int spaceid) throws HDF5LibraryException;

/**
 * H5Sget_select_elem_pointlist returns an array of of element points in the
 * current dataspace selection. The point coordinates have the same
 * dimensionality (rank) as the dataspace they are located within, one
 * coordinate per point.
 * 
 * @param spaceid
 *            Identifier of dataspace to release.
 * @param startpoint
 *            first point to retrieve
 * @param numpoints
 *            number of points to retrieve
 * @param buf
 *            returns points startblock to startblock+num-1, each points is
 *            <i>rank</i> longs.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - buf is null.
 **/
public synchronized static native int H5Sget_select_elem_pointlist(
        int spaceid, long startpoint, long numpoints, long[] buf)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Sget_select_hyper_blocklist returns an array of hyperslab blocks. The
 * block coordinates have the same dimensionality (rank) as the dataspace
 * they are located within. The list of blocks is formatted as follows:
 * 
 * <pre>
 *    <"start" coordinate>, immediately followed by
 *    <"opposite" corner coordinate>, followed by
 *   the next "start" and "opposite" coordinates,
 *   etc.
 *   until all of the selected blocks have been listed.
 * </pre>
 * 
 * @param spaceid
 *            Identifier of dataspace to release.
 * @param startblock
 *            first block to retrieve
 * @param numblocks
 *            number of blocks to retrieve
 * @param buf
 *            returns blocks startblock to startblock+num-1, each block is
 *            <i>rank</i> * 2 (corners) longs.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - buf is null.
 **/
public synchronized static native int H5Sget_select_hyper_blocklist(
        int spaceid, long startblock, long numblocks, long[] buf)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Sget_select_hyper_nblocks returns the number of hyperslab blocks in the
 * current dataspace selection.
 * 
 * @param spaceid
 *            Identifier of dataspace to release.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Sget_select_hyper_nblocks(
        int spaceid) throws HDF5LibraryException;

/**
 * H5Sget_select_npoints determines the number of elements in the current
 * selection of a dataspace.
 * 
 * @param space_id IN: Identifier of the dataspace object to query
 * 
 * @return the number of elements in the selection if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Sget_select_npoints(int space_id)
        throws HDF5LibraryException;

/**
 * H5Sget_select_type retrieves the type of selection currently defined for the dataspace space_id.
 * 
 * @param space_id IN: Identifier of the dataspace object to query
 * 
 * @return the dataspace selection type if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sget_select_type(int space_id) 
        throws HDF5LibraryException;

/**
 * H5Sget_simple_extent_dims returns the size and maximum sizes of each
 * dimension of a dataspace through the dims and maxdims parameters.
 * 
 * @param space_id IN: Identifier of the dataspace object to query
 * @param dims    OUT: Pointer to array to store the size of each dimension.
 * @param maxdims OUT: Pointer to array to store the maximum size of each
 *                     dimension.
 * 
 * @return the number of dimensions in the dataspace if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - dims or maxdims is null.
 **/
public synchronized static native int H5Sget_simple_extent_dims(
        int space_id, long[] dims, long[] maxdims)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Sget_simple_extent_ndims determines the dimensionality (or rank) of a
 * dataspace.
 * 
 * @param space_id IN: Identifier of the dataspace
 * 
 * @return the number of dimensions in the dataspace if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sget_simple_extent_ndims(
        int space_id) throws HDF5LibraryException;

/**
 * H5Sget_simple_extent_npoints determines the number of elements in a
 * dataspace.
 * 
 * @param space_id
 *            ID of the dataspace object to query
 * @return the number of elements in the dataspace if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Sget_simple_extent_npoints(
        int space_id) throws HDF5LibraryException;

/**
 * H5Sget_simple_extent_type queries a dataspace to determine the current
 * class of a dataspace.
 * 
 * @param space_id
 *            Dataspace identifier.
 * 
 * @return a dataspace class name if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sget_simple_extent_type(int space_id)
        throws HDF5LibraryException;

/**
 * H5Sis_simple determines whether a dataspace is a simple dataspace.
 * 
 * @param space_id
 *            Identifier of the dataspace to query
 * 
 * @return true if is a simple dataspace
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native boolean H5Sis_simple(int space_id)
        throws HDF5LibraryException;

/**
 * H5Soffset_simple sets the offset of a simple dataspace space_id.
 * 
 * @param space_id
 *            IN: The identifier for the dataspace object to reset.
 * @param offset
 *            IN: The offset at which to position the selection.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - offset array is null.
 **/
public synchronized static native int H5Soffset_simple(int space_id,
        byte[] offset) throws HDF5LibraryException, NullPointerException;

public synchronized static int H5Soffset_simple(int space_id, long[] offset)
        throws HDF5Exception, NullPointerException
{
    if (offset == null) {
        return -1;
    }

    HDFArray theArray = new HDFArray(offset);
    byte[] theArr = theArray.byteify();

    int retVal = H5Soffset_simple(space_id, theArr);

    theArr = null;
    theArray = null;
    return retVal;
}

/**
 * H5Sselect_all selects the entire extent of the dataspace space_id.
 * 
 * @param space_id
 *            IN: The identifier of the dataspace to be selected.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sselect_all(int space_id)
        throws HDF5LibraryException;

/**
 * H5Sselect_elements selects array elements to be included in the selection
 * for the space_id dataspace.
 * 
 * @param space_id
 *            Identifier of the dataspace.
 * @param op
 *            operator specifying how the new selection is combined.
 * @param num_elements
 *            Number of elements to be selected.
 * @param coord
 *            A 2-dimensional array specifying the coordinates of the
 *            elements.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
private synchronized static native int H5Sselect_elements(int space_id,
        int op, int num_elements, byte[] coord)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Sselect_elements selects array elements to be included in the selection
 * for the space_id dataspace.
 * 
 * @param space_id
 *            Identifier of the dataspace.
 * @param op
 *            operator specifying how the new selection is combined.
 * @param num_elements
 *            Number of elements to be selected.
 * @param coord2D
 *            A 2-dimensional array specifying the coordinates of the
 *            elements.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5Exception
 *                - Error in the data conversion
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - cord array is
 **/
public synchronized static int H5Sselect_elements(int space_id, int op,
        int num_elements, long[][] coord2D)
        throws HDF5Exception, HDF5LibraryException, NullPointerException
{
    if (coord2D == null) {
        return -1;
    }

    HDFArray theArray = new HDFArray(coord2D);
    byte[] coord = theArray.byteify();

    int retVal = H5Sselect_elements(space_id, op, num_elements, coord);

    coord = null;
    theArray = null;
    return retVal;
}

///**
//*  H5Sselect_hyperslab selects a hyperslab region to add to
//*  the current selected region for the dataspace specified
//*  by space_id.  The start, stride, count, and block arrays
//*  must be the same size as the rank of the dataspace.
//*
//*  @param space_id IN: Identifier of dataspace selection to modify
//*  @param op       IN: Operation to perform on current selection.
//*  @param start    IN: Offset of start of hyperslab
//*  @param count    IN: Number of blocks included in hyperslab.
//*  @param stride   IN: Hyperslab stride.
//*  @param block    IN: Size of block in hyperslab.
//*
//*  @return none
//*
//*  @exception HDF5LibraryException - Error from the HDF-5 Library.
//*  @exception NullPointerException - an input array is null.
//*  @exception IllegalArgumentException - an input array is invalid.
//**/
//public synchronized static native void H5Sselect_hyperslab(int space_id, H5S_SELECT_OPER op,
//    long start[], long _stride[], long count[], long _block[])
//  throws HDF5LibraryException, NullPointerException, IllegalArgumentException;
//public synchronized static native int H5Scombine_hyperslab(int space_id, H5S_SELECT_OPER op,
//    const long start[], const long _stride[],
//    const long count[], const long _block[])
//  throws HDF5LibraryException, NullPointerException;
//public synchronized static native int H5Sselect_select(int space1_id, H5S_SELECT_OPER op,
//    int space2_id)
//  throws HDF5LibraryException, NullPointerException;
//public synchronized static native int H5Scombine_select(int space1_id, H5S_SELECT_OPER op,
//    int space2_id)
//  throws HDF5LibraryException, NullPointerException;

/**
 * H5Sselect_hyperslab selects a hyperslab region to add to the current
 * selected region for the dataspace specified by space_id. The start,
 * stride, count, and block arrays must be the same size as the rank of the
 * dataspace.
 * 
 * @param space_id
 *            IN: Identifier of dataspace selection to modify
 * @param op
 *            IN: Operation to perform on current selection.
 * @param start
 *            IN: Offset of start of hyperslab
 * @param stride
 *            IN: Hyperslab stride.
 * @param count
 *            IN: Number of blocks included in hyperslab.
 * @param block
 *            IN: Size of block in hyperslab.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 * @exception NullPointerException
 *                - an input array is null.
 * @exception NullPointerException
 *                - an input array is invalid.
 **/
public synchronized static int H5Sselect_hyperslab(int space_id,
        int op, byte[] start, byte[] stride, byte[] count, byte[] block)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException
{
    ByteBuffer startbb = ByteBuffer.wrap(start);
    long[] lastart = (startbb.asLongBuffer()).array();
    ByteBuffer stridebb = ByteBuffer.wrap(stride);
    long[] lastride = (stridebb.asLongBuffer()).array();
    ByteBuffer countbb = ByteBuffer.wrap(count);
    long[] lacount = (countbb.asLongBuffer()).array();
    ByteBuffer blockbb = ByteBuffer.wrap(block);
    long[] lablock = (blockbb.asLongBuffer()).array();

    return H5Sselect_hyperslab(space_id, op, lastart, lastride, lacount, lablock);
}

public synchronized static native int H5Sselect_hyperslab(int space_id, int op,
        long[] start, long[] stride, long[] count, long[] block)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 * H5Sselect_none resets the selection region for the dataspace space_id to
 * include no elements.
 * 
 * @param space_id
 *            IN: The identifier of the dataspace to be reset.
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sselect_none(int space_id)
        throws HDF5LibraryException;

/**
 * H5Sselect_valid verifies that the selection for the dataspace.
 * 
 * @param space_id
 *            The identifier for the dataspace in which the selection is
 *            being reset.
 * 
 * @return true if the selection is contained within the extent and FALSE if
 *         it is not or is an error.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native boolean H5Sselect_valid(int space_id)
        throws HDF5LibraryException;

/**
 * H5Sset_extent_none removes the extent from a dataspace and sets the type
 * to H5S_NONE.
 * 
 * @param space_id
 *            The identifier for the dataspace from which the extent is to
 *            be removed.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sset_extent_none(int space_id)
        throws HDF5LibraryException;

/**
 * H5Sset_extent_simple sets or resets the size of an existing dataspace.
 * 
 * @param space_id
 *            Dataspace identifier.
 * @param rank
 *            Rank, or dimensionality, of the dataspace.
 * @param current_size
 *            Array containing current size of dataspace.
 * @param maximum_size
 *            Array containing maximum size of dataspace.
 * 
 * @return a dataspace identifier if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Sset_extent_simple(int space_id,
        int rank, long[] current_size, long[] maximum_size)
        throws HDF5LibraryException, NullPointerException;

public synchronized static  int H5Sset_extent_simple(int space_id,
        int rank, byte[] current_size, byte[] maximum_size)
        throws HDF5LibraryException, NullPointerException
{
    ByteBuffer csbb = ByteBuffer.wrap(current_size);
    long[] lacs = (csbb.asLongBuffer()).array();
    ByteBuffer maxsbb = ByteBuffer.wrap(maximum_size);
    long[] lamaxs = (maxsbb.asLongBuffer()).array();

    return H5Sset_extent_simple(space_id, rank, lacs, lamaxs);
}

//////////////////////////////////////////////////////////////
////
//H5T: Datatype Interface Functions //
////
//////////////////////////////////////////////////////////////

/**
 *  H5Tarray_create creates a new array datatype object. 
 *
 *  @deprecated As of HDF5 1.8, replaced by {@link #H5Tarray_create(int, int, long[])}
 *
 *  @param base     IN: Datatype identifier for the array base datatype.
 *  @param rank     IN: Rank of the array.
 *  @param dims     IN: Size of each array dimension.
 *  @param perms    IN: Dimension permutation. (Currently not implemented.)
 *
 *  @return a valid datatype identifier
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - dims is null.
 **/
@Deprecated
public static int H5Tarray_create(int base, int rank, int[] dims,
        int[] perms) throws HDF5LibraryException, NullPointerException
{
    int id = _H5Tarray_create(base, rank, dims, perms);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Tarray_create(int base, int rank,
        int[] dims, int[] perms)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tarray_create creates a new array datatype object. 
 *
 *  @param base_id  IN: Datatype identifier for the array base datatype.
 *  @param ndims    IN: Rank of the array.
 *  @param dim      IN: Size of each array dimension.
 *
 *  @return a valid datatype identifier
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - dim is null.
 **/
public static int H5Tarray_create(int base_id, int ndims, long[] dim)
throws HDF5LibraryException, NullPointerException
{
    int id = _H5Tarray_create2(base_id, ndims, dim);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Tarray_create2(int base_id, int ndims, long[] dim)
throws HDF5LibraryException, NullPointerException;

/**
 * H5Tclose releases a datatype.
 * 
 * @param type_id IN: Identifier of datatype to release.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tclose(int type_id) throws HDF5LibraryException
{
    if (type_id < 0)
        throw new HDF5LibraryException("Negative ID");;
    
    OPEN_IDS.removeElement(type_id);
    return _H5Tclose(type_id);
}

private synchronized static native int _H5Tclose(int type_id)
        throws HDF5LibraryException;
/**
* H5Tcommit commits a transient datatype (not immutable) to a file, turned
* it into a named datatype.
* 
* @deprecated As of HDF5 1.8, replaced by {@link #H5Tcommit(int, String, int, int, int, int)}
*
* @param loc_id   IN: Location identifier.
* @param name     IN: Name given to committed datatype.
* @param type_id  IN: Identifier of datatype to be committed.
* 
* @return a non-negative value if successful
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
* @exception NullPointerException - name is null.
**/
@Deprecated
public static int H5Tcommit(int loc_id, String name,
       int type) throws HDF5LibraryException, NullPointerException
{
   return H5Tcommit1(loc_id, name, type);
}
public synchronized static native int H5Tcommit1(int loc_id, String name,
       int type) throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tcommit saves a transient datatype as an immutable named datatype in a file.
 *
 *  @param loc_id   IN: Location identifier.
 *  @param name     IN: Name given to committed datatype.
 *  @param type_id  IN: Identifier of datatype to be committed.
 *  @param lcpl_id  IN: Link creation property list.
 *  @param tcpl_id  IN: Datatype creation property list.
 *  @param tapl_id  IN: Datatype access property list.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native void H5Tcommit(int loc_id, String name, int type_id, int lcpl_id,
        int tcpl_id, int tapl_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tcommit_anon commits a transient datatype (not immutable) to a file, 
 *  turning it into a named datatype with the specified creation and property lists.
 *
 *  @param loc_id   IN: Location identifier.
 *  @param type_id  IN: Identifier of datatype to be committed.
 *  @param tcpl_id  IN: Datatype creation property list.
 *  @param tapl_id  IN: Datatype access property list.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tcommit_anon(int loc_id, int type_id, int tcpl_id, int tapl_id)
        throws HDF5LibraryException;

/**
* H5Tcommitted queries a type to determine whether the type specified by
* the type identifier is a named type or a transient type.
* 
* @param type_id   IN: Identifier of datatype.
* 
* @return true the datatype has been committed
* 
* @exception HDF5LibraryException - Error from the HDF-5 Library.
**/
public synchronized static native boolean H5Tcommitted(int type)
        throws HDF5LibraryException;

/**
 *  H5Tcompiler_conv finds out whether the library's conversion function from 
 *  type src_id to type dst_id is a compiler (hard) conversion.
 *
 *  @param src_id     IN: Identifier of source datatype.
 *  @param dst_id     IN: Identifier of destination datatype.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tcompiler_conv(int src_id, int dst_id)
throws HDF5LibraryException;

/**
 **  H5Tconvert converts nelmts elements from the type specified by the src_id identifier to type dst_id.
 *
 *  @param src_id     IN: Identifier of source datatype.
 *  @param dst_id     IN: Identifier of destination datatype.
 *  @param nelmts     IN: Size of array buf.
 *  @param buf        IN: Array containing pre- and post-conversion values.
 *  @param background IN: Optional background buffer.
 *  @param plist_id   IN: Dataset transfer property list identifier.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native void H5Tconvert(int src_id, int dst_id, long nelmts, byte[] buf,
        byte[] background, int plist_id)
        throws HDF5LibraryException, NullPointerException;
//  int H5Tconvert(int src_id, int dst_id, long nelmts, Pointer buf, Pointer background, int plist_id);

/**
 *  H5Tcopy copies an existing datatype. The returned type is
 *  always transient and unlocked.
 *
 *  @param type_id IN: Identifier of datatype to copy. Can be a datatype 
 *                      identifier, a  predefined datatype (defined in 
 *                      H5Tpublic.h), or a dataset Identifier.
 *
 *  @return a datatype identifier if successful
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tcopy(int type_id) throws HDF5LibraryException
{
    int id = _H5Tcopy(type_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Tcopy(int type_id)
        throws HDF5LibraryException;
/**
 * H5Tcreate creates a new dataype of the specified class with the specified
 * number of bytes.
 * 
 * @param type IN: Class of datatype to create.
 * @param size IN: The number of bytes in the datatype to create.
 * 
 * @return datatype identifier if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tcreate(int dclass, int size)
        throws HDF5LibraryException
{
    return H5Tcreate(dclass, (long)size);
}

/**
 *  H5Tcreate creates a new dataype of the specified class with
 *  the specified number of bytes.
 *
 *  @param type IN: Class of datatype to create.
 *  @param size IN: The number of bytes in the datatype to create.
 *
 *  @return datatype identifier
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tcreate(int type, long size)
    throws HDF5LibraryException
{
    int id = _H5Tcreate(type, size);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Tcreate(int type, long size)
throws HDF5LibraryException;

/**
 *  H5Tdecode reconstructs the HDF5 data type object and 
 *  returns a new object handle for it.
 *
 *  @param buf   IN: Buffer for the data type object to be decoded.
 *
 *  @return a new object handle
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native int H5Tdecode(byte[] buf)
throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tdetect_class determines whether the datatype specified in dtype_id contains 
 *  any datatypes of the datatype class specified in dtype_class. 
 *
 *  @param type_id  IN: Identifier of datatype to query.
 *  @param cls      IN: Identifier of datatype cls.
 *
 *  @return true if the datatype specified in dtype_id contains any datatypes of the datatype class
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native boolean H5Tdetect_class(int type_id, int cls)
throws HDF5LibraryException;

/**
 *  H5Tencode converts a data type description into binary form in a buffer.
 *
 *  @param obj_id   IN: Identifier of the object to be encoded.
 *  @param buf     OUT: Buffer for the object to be encoded into. 
 *                      If the provided buffer is NULL, only the 
 *                      size of buffer needed is returned.
 *  @param nalloc   IN: The size of the allocated buffer.
 *
 *  @return the size needed for the allocated buffer.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - buf is null.
 **/
public synchronized static native int H5Tencode(int obj_id, byte[] buf, long nalloc)
throws HDF5LibraryException, NullPointerException;
///**
// *  H5Tencode converts a data type description into binary form in a buffer.
// *
// *  @param obj_id   IN: Identifier of the object to be encoded.
// *
// *  @return the buffer for the object to be encoded into.
// *
// *  @exception HDF5LibraryException - Error from the HDF-5 Library.
// **/
//public synchronized static native byte[] H5Tencode(int obj_id)
//throws HDF5LibraryException;

/**
 * H5Tenum_create creates a new enumeration datatype based on the specified
 * base datatype, parent_id, which must be an integer type.
 * 
 *  @param base_id IN: Identifier of the parent datatype to release.
 *
 *  @return the datatype identifier for the new enumeration datatype
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tenum_create(int base_id) throws HDF5LibraryException
{
    int id = _H5Tenum_create(base_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Tenum_create(int base_id)
        throws HDF5LibraryException;

/**
 *  H5Tenum_insert inserts a new enumeration datatype member
 *  into an enumeration datatype.
 *
 *  @param type  IN: Identifier of datatype.
 *  @param name  IN: The name of the member
 *  @param value IN: The value of the member, data of the correct type
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native void H5Tenum_insert(int type, String name, byte[] value)
throws HDF5LibraryException, NullPointerException;

/**
 * H5Tenum_insert inserts a new enumeration datatype member into an
 * enumeration datatype.
 * 
 * @param type  IN: Identifier of datatype.
 * @param name  IN: The name of the member
 * @param value IN: The value of the member, data of the correct type
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - name is null.
 **/
public static int H5Tenum_insert(int type, String name,
        int[] value) throws HDF5LibraryException, NullPointerException
{
    return H5Tenum_insert_int(type, name, value);
}

public static int H5Tenum_insert(int type, String name,
        int value) throws HDF5LibraryException, NullPointerException
{
    int[] val = { value };
    return H5Tenum_insert_int(type, name, val);
}

private synchronized static native int H5Tenum_insert_int(int type, String name,
        int[] value) throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tenum_nameof finds the symbol name that corresponds
 *  to the specified value of the enumeration datatype type.
 *
 *  @param type   IN: Identifier of datatype.
 *  @param value  IN: The value of the member, data of the correct
 *  @param size   IN: The probable length of the name
 *
 *  @return the symbol name.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - value is null.
 **/
public synchronized static native String H5Tenum_nameof(int type, byte[] value, long size)
throws HDF5LibraryException, NullPointerException;
//int H5Tenum_nameof(int type, Pointer value, Buffer name/* out */, long size);

/**
 * H5Tenum_nameof finds the symbol name that corresponds to the specified
 * value of the enumeration datatype type.
 * 
 * @param type  IN: Identifier of datatype.
 * @param value IN: The value of the member, data of the correct
 * @param name OUT: The name of the member
 * @param size  IN: The max length of the name
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - name is null.
 **/
public static int H5Tenum_nameof(int type, int[] value,
        String[] name, int size)
        throws HDF5LibraryException, NullPointerException
{
    return H5Tenum_nameof_int(type, value, name, size);
}
private synchronized static native int H5Tenum_nameof_int(int type, int[] value,
        String[] name, int size)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tenum_valueof finds the value that corresponds to
 *  the specified name of the enumeration datatype type.
 *
 *  @param type   IN: Identifier of datatype.
 *  @param name   IN: The name of the member
 *  @param value OUT: The value of the member
 *
 *  @return the value of the enumeration datatype.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tenum_valueof(int type, String name, byte[] value)
throws HDF5LibraryException, NullPointerException;

/**
 * H5Tenum_valueof finds the value that corresponds to the specified name of
 * the enumeration datatype type.
 * 
 * @param type   IN: Identifier of datatype.
 * @param name   IN: The name of the member
 * @param value OUT: The value of the member
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - name is null.
 **/
public static int H5Tenum_valueof(int type,
        String name, int[] value)
        throws HDF5LibraryException, NullPointerException
{
    return H5Tenum_valueof_int(type, name, value);
}
private synchronized static native int H5Tenum_valueof_int(int type,
        String name, int[] value)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tequal determines whether two datatype identifiers refer
 *  to the same datatype.
 *
 *  @param type_id1 IN: Identifier of datatype to compare.
 *  @param type_id2 IN: Identifier of datatype to compare.
 *
 *  @return true if the datatype identifiers refer to the
 *  same datatype, else false.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native boolean H5Tequal(int type_id1,
        int type_id2) throws HDF5LibraryException;

//public interface H5T_conv_t extends Callback {
//int callback(int src_id, int dst_id, H5T_cdata_t cdata, long nelmts,
//long buf_stride, long bkg_stride, Pointer buf, Pointer bkg,
//int dset_xfer_plist);
//}
//
////Exception handler. If an exception like overflow happenes during
////conversion,
////this function is called if it's registered through H5Pset_type_conv_cb.
//public interface H5T_conv_except_func_t extends Callback {
//int callback(H5T_conv_except_t except_type, int src_id, int dst_id,
//Pointer src_buf, Pointer dst_buf, Pointer user_data);
//}

//H5T_conv_t H5Tfind(int src_id, int dst_id, H5T_cdata_t *pcdata);

/**
 *  H5Tget_array_dims returns the sizes of the dimensions of the specified array datatype object. 
 *
 *  @deprecated As of HDF5 1.8
 *
 *  @param type_id  IN: Datatype identifier of array object.
 *  @param dims    OUT: Sizes of array dimensions.
 *  @param perm[]  OUT: Dimension permutations. (This parameter is not used.)
 *
 *  @return the non-negative number of dimensions of the array type
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - dims is null.
 **/
@Deprecated
public synchronized static native int H5Tget_array_dims(int dt, int[] dims,
        int[] perms) throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tget_array_dims returns the sizes of the dimensions of the specified array datatype object. 
 *
 *  @deprecated As of HDF5 1.8, replaced by {@link #H5Tget_array_dims(int, long[])}
 *
 *  @param type_id  IN: Datatype identifier of array object.
 *  @param dims    OUT: Sizes of array dimensions.
 *  @param perm    OUT: Dimension permutation. (Currently not implemented.)
 *
 *  @return the non-negative number of dimensions of the array type
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
@Deprecated
public static int H5Tget_array_dims(int type_id, long[] dims, int[] perm)
throws HDF5LibraryException, NullPointerException
{
  return H5Tget_array_dims1(type_id, dims, perm);
}
/**
 *  H5Tget_array_dims1 returns the sizes of the dimensions of the specified array datatype object. 
 *
 *  @deprecated As of HDF5 1.8, replaced by {@link #H5Tget_array_dims2(int, long[])}
 *
 *  @see public static int H5Tget_array_dims(int type_id, long[] dims, int[] perm)
 **/
@Deprecated
private synchronized static native int H5Tget_array_dims1(int type_id, long[] dims, int[] perm)
throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tget_array_dims returns the sizes of the dimensions of the specified array datatype object. 
 *
 *  @param type_id  IN: Datatype identifier of array object.
 *  @param dims    OUT: Sizes of array dimensions.
 *
 *  @return the non-negative number of dimensions of the array type
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - dims is null.
 **/
public static int H5Tget_array_dims(int type_id, long[] dims)
throws HDF5LibraryException, NullPointerException
{
  return H5Tget_array_dims2(type_id, dims);
}
/**
 *  H5Tget_array_dims2 returns the sizes of the dimensions of the specified array datatype object. 
 *
 *  @see public static int H5Tget_array_dims(int type_id, long[] dims)
 **/
public synchronized static native int H5Tget_array_dims2(int type_id, long[] dims)
throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tget_array_ndims returns the rank, the number of dimensions, of an array datatype object. 
 *
 *  @param type_id  IN: Datatype identifier of array object.
 *
 *  @return the rank of the array
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_array_ndims(int type_id)
throws HDF5LibraryException;

/**
 * H5Tget_class returns the datatype class identifier.
 * 
 *  @param type_id  IN: Identifier of datatype to query.
 *
 *  @return datatype class identifier if successful; otherwise H5T_NO_CLASS(-1).
 * 
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_class(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tget_class_name returns the datatype class identifier.
 * 
 *  @param class_id  IN: Identifier of class from H5Tget_class.
 *
 *  @return class name if successful; otherwise H5T_NO_CLASS.
 * 
 **/
public static String H5Tget_class_name(int class_id)
{
    String retValue = null;
    if(HDF5Constants.H5T_INTEGER==class_id)  /*integer types             */
        retValue = "H5T_INTEGER";
    else if(HDF5Constants.H5T_FLOAT==class_id)    /*floating-point types      */
        retValue = "H5T_FLOAT";
    else if(HDF5Constants.H5T_TIME==class_id)     /*date and time types       */
        retValue = "H5T_TIME";
    else if(HDF5Constants.H5T_STRING==class_id)   /*character string types    */
        retValue = "H5T_STRING";
    else if(HDF5Constants.H5T_BITFIELD==class_id) /*bit field types           */
        retValue = "H5T_BITFIELD";
    else if(HDF5Constants.H5T_OPAQUE==class_id)   /*opaque types              */
        retValue = "H5T_OPAQUE";
    else if(HDF5Constants.H5T_COMPOUND==class_id) /*compound types           */
        retValue = "H5T_COMPOUND";
    else if(HDF5Constants.H5T_REFERENCE==class_id)/*reference types          */
        retValue = "H5T_REFERENCE";
    else if(HDF5Constants.H5T_ENUM==class_id)     /*enumeration types        */
        retValue = "H5T_ENUM";
    else if(HDF5Constants.H5T_VLEN==class_id)     /*Variable-Length types    */
        retValue = "H5T_VLEN";
    else if(HDF5Constants.H5T_ARRAY==class_id)    /*Array types              */
        retValue = "H5T_ARRAY";
    else
        retValue = "H5T_NO_CLASS";

    return retValue;
}

/**
 *  H5Tget_create_plist returns a property list identifier for the datatype 
 *  creation property list associated with the datatype specified by type_id. 
 *
 *  @param type_id   IN: Identifier of datatype.
 *
 *  @return a datatype property list identifier.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_create_plist(int type_id)
throws HDF5LibraryException;

/**
 * H5Tget_cset retrieves the character set type of a string datatype.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return a valid character set type if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_cset(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_cset the character set to be used.
 * 
 * @param type_id  IN: Identifier of datatype to modify.
 * @param cset     IN: Character set type.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_cset(int type_id, int cset)
        throws HDF5LibraryException;

/**
 * H5Tget_ebias retrieves the exponent bias of a floating-point type.
 * 
 * @param type_id
 *            Identifier of datatype to query.
 * 
 * @return the bias if successful; otherwise 0.
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_ebias(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_ebias sets the exponent bias of a floating-point type.
 * 
 * @param type_id
 *            Identifier of datatype to set.
 * @param ebias
 *            Exponent bias value.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Tset_ebias(int type_id, int ebias)
        throws HDF5LibraryException
{
    H5Tset_ebias(type_id, (long)ebias);
    return 0;
}

/**
 *  H5Tget_ebias retrieves the exponent bias of a
 *  floating-point type.
 *
 *  @param type_id  IN: Identifier of datatype to query.
 *
 *  @return the bias 
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Tget_ebias_long(int type_id)
throws HDF5LibraryException;

/**
 *  H5Tset_ebias sets the exponent bias of a floating-point type.
 *
 *  @param type_id  IN: Identifier of datatype to set.
 *  @param ebias    IN: Exponent bias value.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tset_ebias(int type_id, long ebias)
throws HDF5LibraryException;

/**
 *  H5Tget_fields retrieves information about the locations of
 *  the various bit fields of a floating point datatype.
 *
 *  @param type_id  IN: Identifier of datatype to query.
 *  @param fields  OUT: location of size and bit-position.
 *  <ul>
 *      <li>fields[0] = spos  OUT: location to return size of in bits.</li>
 *      <li>fields[1] = epos  OUT: location to return exponent bit-position.</li>
 *      <li>fields[2] = esize OUT: location to return size of exponent in bits.</li>
 *      <li>fields[3] = mpos  OUT: location to return mantissa bit-position.</li>
 *      <li>fields[4] = msize OUT: location to return size of mantissa in bits.</li>
 *  </ul>
 *
 *  @return none.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - fields is null.
 *  @exception IllegalArgumentException - fields array is invalid.
 **/
public synchronized static native void H5Tget_fields(int type_id, long[] fields)
throws HDF5LibraryException, NullPointerException, IllegalArgumentException;

/**
 * H5Tget_fields retrieves information about the locations of the various
 * bit fields of a floating point datatype.
 * 
 * @param type_id IN: Identifier of datatype to query.
 * @param fields OUT: location of size and bit-position.
 * 
 * <pre>
 *      fields[0] = spos  OUT: location to return size of in bits.
 *      fields[1] = epos  OUT: location to return exponent bit-position.
 *      fields[2] = esize OUT: location to return size of exponent in bits.
 *      fields[3] = mpos  OUT: location to return mantissa bit-position.
 *      fields[4] = msize OUT: location to return size of mantissa in bits.
 * </pre>
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - fields is null.
 * @exception IllegalArgumentException - fields array is invalid.
 **/
public static int H5Tget_fields(int type_id, int[] fields)
        throws HDF5LibraryException, NullPointerException, IllegalArgumentException
{
    return H5Tget_fields_int(type_id, fields);
}
private synchronized static native int H5Tget_fields_int(int type_id,
        int[] fields)
        throws HDF5LibraryException, NullPointerException,
        IllegalArgumentException;

/**
 *  H5Tset_fields sets the locations and sizes of the various
 *  floating point bit fields.
 *
 *  @param type_id  IN: Identifier of datatype to set.
 *  @param spos     IN: Size position.
 *  @param epos     IN: Exponent bit position.
 *  @param esize    IN: Size of exponent in bits.
 *  @param mpos     IN: Mantissa bit position.
 *  @param msize    IN: Size of mantissa in bits.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tset_fields(int type_id, long spos, long epos, long esize, long mpos, long msize)
throws HDF5LibraryException;

/**
 * H5Tset_fields sets the locations and sizes of the various floating point
 * bit fields.
 * 
 * @param type_id
 *            Identifier of datatype to set.
 * @param spos
 *            Size position.
 * @param epos
 *            Exponent bit position.
 * @param esize
 *            Size of exponent in bits.
 * @param mpos
 *            Mantissa bit position.
 * @param msize
 *            Size of mantissa in bits.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Tset_fields(int type_id, int spos,
        int epos, int esize, int mpos, int msize)
        throws HDF5LibraryException
{
    H5Tset_fields(type_id, (long)spos, (long)epos, (long)esize, 
            (long)mpos, (long)msize);
    return 0;
}

/**
 * H5Tget_inpad retrieves the internal padding type for unused bits in
 * floating-point datatypes.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return a valid padding type if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_inpad(int type_id)
        throws HDF5LibraryException;

/**
 * If any internal bits of a floating point type are unused (that is, those
 * significant bits which are not part of the sign, exponent, or mantissa),
 * then H5Tset_inpad will be filled according to the value of the padding
 * value property inpad.
 * 
 * @param type_id  IN: Identifier of datatype to modify.
 * @param inpad    IN: Padding type.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_inpad(int type_id, int inpad)
        throws HDF5LibraryException;

/**
 * H5Tget_member_class returns the datatype of the specified member.
 * 
 * @param type_id   IN: Datatype identifier of compound object.
 * @param membno    IN: Compound object member number.
 * 
 * @return the identifier of a copy of the datatype of the field if successful;
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_member_class(int type_id,
        int membno) throws HDF5LibraryException;

/**
 * H5Tget_member_index retrieves the index of a field of a compound
 * datatype.
 * 
 * @param type_id    IN: Identifier of datatype to query.
 * @param field_name IN: Field name of the field index to retrieve.
 * 
 * @return if field is defined, the index; else negative.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_member_index(int type_id, String field_name);

/**
 * H5Tget_member_name retrieves the name of a field of a compound datatype or 
 * an element of an enumeration datatype. 
 * 
 * @param type_id    IN: Identifier of datatype to query.
 * @param field_idx  IN: Field index (0-based) of the field name to retrieve.
 * 
 * @return a valid pointer to the name if successful; otherwise null.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native String H5Tget_member_name(int type_id, int field_idx);

/**
 * H5Tget_member_offset returns the byte offset of the specified member of
 * the compound datatype. This is the byte offset in the HDF-5 file/library,
 * NOT the offset of any Java object which might be mapped to this data
 * item.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * @param membno   IN: Field index (0-based) of the field type to retrieve.
 * 
 * @return the offset of the member.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Tget_member_offset(int type_id,
        int membno) throws HDF5LibraryException;

/**
 * H5Tget_member_type returns the datatype of the specified member.
 * 
 * @param type_id   IN: Identifier of datatype to query.
 * @param field_idx IN: Field index (0-based) of the field type to retrieve.
 * 
 * @return the identifier of a copy of the datatype of the field if
 *         successful;
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tget_member_type(int type_id, int field_idx)
        throws HDF5LibraryException
{
    int id = _H5Tget_member_type(type_id, field_idx);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Tget_member_type(int type_id,
        int field_idx) throws HDF5LibraryException;

/**
 *  H5Tget_member_value returns the value of the enumeration datatype member memb_no. 
 *
 *  @param type_id  IN: Datatype identifier for the enumeration datatype.
 *  @param membno   IN: Number of the enumeration datatype member.
 *  @param value   OUT: The value of the member
 *
 *  @return none.
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - value is null.
 **/
public synchronized static native void H5Tget_member_value(int type_id, int membno, byte[] value)
throws HDF5LibraryException, NullPointerException;

/**
 * H5Tget_member_value returns the value of the enumeration datatype member
 * memb_no.
 * 
 * @param type_id IN: Identifier of datatype.
 * @param membno  IN: The name of the member
 * @param value  OUT: The value of the member
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - value is null.
 **/
public static int H5Tget_member_value(int type_id,
        int membno, int[] value)
        throws HDF5LibraryException, NullPointerException
{
    return H5Tget_member_value_int(type_id, membno, value);
}
private synchronized static native int H5Tget_member_value_int(int type_id,
        int membno, int[] value)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tget_native_type returns the equivalent native datatype for the datatype specified in type_id. 
 *
 *  @param type_id   IN: Identifier of datatype to query.
 *                       Direction of search is assumed to be in ascending order.
 *
 *  @return the native datatype identifier for the specified dataset datatype. 
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static int H5Tget_native_type(int tid)
        throws HDF5LibraryException 
{
    return H5Tget_native_type(tid, HDF5Constants.H5T_DIR_ASCEND);
}

/**
 *  H5Tget_native_type returns the equivalent native datatype for the datatype specified in type_id. 
 *
 *  @param type_id   IN: Identifier of datatype to query.
 *  @param direction IN: Direction of search.
 *
 *  @return the native datatype identifier for the specified dataset datatype. 
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tget_native_type(int tid, int direction)
        throws HDF5LibraryException
{
    int id = _H5Tget_native_type(tid, direction);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}
private synchronized static native int _H5Tget_native_type(int tid,
        int direction) throws HDF5LibraryException;

/**
 * H5Tget_nmembers retrieves the number of fields a compound datatype has.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return number of members datatype has if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_nmembers(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tget_norm retrieves the mantissa normalization of a floating-point
 * datatype.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return a valid normalization type if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_norm(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_norm sets the mantissa normalization of a floating-point datatype.
 * 
 * @param type_id  IN: Identifier of datatype to set.
 * @param norm     IN: Mantissa normalization type.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_norm(int type_id, int norm)
        throws HDF5LibraryException;

/**
 * H5Tget_offset retrieves the bit offset of the first significant bit.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 *
 * @return a positive offset value if successful; otherwise 0.
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_offset(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_offset sets the bit offset of the first significant bit.
 * 
 * @param type_id
 *            Identifier of datatype to set.
 * @param offset
 *            Offset of first significant bit.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Tset_offset(int type_id, int offset)
        throws HDF5LibraryException
{
    H5Tset_offset(type_id, (long)offset);
    return 0;
}

/**
 *  H5Tset_offset sets the bit offset of the first significant bit.
 *
 *  @param type_id  IN: Identifier of datatype to set.
 *  @param offset   IN: Offset of first significant bit.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tset_offset(int type_id, long offset)
throws HDF5LibraryException;

/**
 * H5Tget_order returns the byte order of an atomic datatype.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return a byte order constant if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_order(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_order sets the byte ordering of an atomic datatype.
 * 
 * @param type_id  IN: Identifier of datatype to set.
 * @param order    IN: Byte ordering constant.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_order(int type_id, int order)
        throws HDF5LibraryException;

/**
 * H5Tget_pad retrieves the padding type of the least and most-significant
 * bit padding.
 * 
 * @param type_id IN: Identifier of datatype to query.
 * @param pad    OUT: locations to return least-significant and
 *                    most-significant bit padding type.
 * 
 *            <pre>
 *      pad[0] = lsb // least-significant bit padding type
 *      pad[1] = msb // most-significant bit padding type
 * </pre>
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 * @exception NullPointerException - pad is null.
 **/
public synchronized static native int H5Tget_pad(int type_id, int[] pad)
        throws HDF5LibraryException, NullPointerException;

/**
 * H5Tset_pad sets the least and most-significant bits padding types.
 * 
 * @param type_id  IN: Identifier of datatype to set.
 * @param lsb      IN: Padding type for least-significant bits.
 * @param msb      IN: Padding type for most-significant bits.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_pad(int type_id, int lsb,
        int msb) throws HDF5LibraryException;

/**
 * H5Tget_precision returns the precision of an atomic datatype.
 * 
 * @param type_id
 *            Identifier of datatype to query.
 * 
 * @return the number of significant bits if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_precision(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_precision sets the precision of an atomic datatype.
 * 
 * @param type_id
 *            Identifier of datatype to set.
 * @param precision
 *            Number of bits of precision for datatype.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Tset_precision(int type_id,
        int precision) throws HDF5LibraryException
{
    H5Tset_precision(type_id, (long)precision);
    return 0;
}

/**
 *  H5Tget_precision returns the precision of an atomic datatype.
 *
 *  @param type_id  IN: Identifier of datatype to query.
 *
 *  @return the number of significant bits if successful
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Tget_precision_long(int type_id)
throws HDF5LibraryException;

/**
 *  H5Tset_precision sets the precision of an atomic datatype.
 *
 *  @param type_id    IN: Identifier of datatype to set.
 *  @param precision  IN: Number of bits of precision for datatype.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tset_precision(int type_id, long precision)
throws HDF5LibraryException;

/**
 * H5Tget_sign retrieves the sign type for an integer type.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return a valid sign type if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_sign(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_sign sets the sign proprety for an integer type.
 * 
 * @param type_id  IN: Identifier of datatype to set.
 * @param sign     IN: Sign type.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_sign(int type_id, int sign)
        throws HDF5LibraryException;

/**
 * H5Tget_size returns the size of a datatype in bytes.
 * 
 * @param type_id
 *            Identifier of datatype to query.
 * 
 * @return the size of the datatype in bytes if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_size(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_size sets the total size in bytes, size, for an atomic datatype
 * (this operation is not permitted on compound datatypes).
 * 
 * @param type_id
 *            Identifier of datatype to change size.
 * @param size
 *            Size in bytes to modify datatype.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException
 *                - Error from the HDF-5 Library.
 **/
public static int H5Tset_size(int type_id, int size)
        throws HDF5LibraryException
{
    H5Tset_size(type_id, (long)size);
    return 0;
}

/**
 *  H5Tget_size returns the size of a datatype in bytes.
 *
 *  @param type_id  IN: Identifier of datatype to query.
 *
 *  @return the size of the datatype in bytes
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native long H5Tget_size_long(int type_id)
throws HDF5LibraryException;

/**
 *  H5Tset_size sets the total size in bytes, size, for an
 *  atomic datatype (this operation is not permitted on
 *  compound datatypes).
 *
 *  @param type_id  IN: Identifier of datatype to change size.
 *  @param size     IN: Size in bytes to modify datatype.
 *
 *  @return none
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native void H5Tset_size(int type_id, long size)
throws HDF5LibraryException;

/**
 * H5Tget_strpad retrieves the string padding method for a string datatype.
 * 
 * @param type_id  IN: Identifier of datatype to query.
 * 
 * @return a valid string padding type if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tget_strpad(int type_id)
        throws HDF5LibraryException;

/**
 * H5Tset_strpad defines the storage mechanism for the string.
 * 
 * @param type_id IN: Identifier of datatype to modify.
 * @param strpad  IN: String padding type.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_strpad(int type_id, int strpad)
        throws HDF5LibraryException;

/**
 * H5Tget_super returns the type from which TYPE is derived.
 * 
 * @param type IN: Identifier of datatype.
 * 
 * @return the parent type
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tget_super(int type) throws HDF5LibraryException
{
    int id = _H5Tget_super(type);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Tget_super(int type)
        throws HDF5LibraryException;

/**
 * H5Tget_tag returns the tag associated with datatype type_id.
 * 
 * @param type IN: Identifier of datatype.
 * 
 * @return the tag
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native String H5Tget_tag(int type)
        throws HDF5LibraryException;

/**
 * H5Tset_tag tags an opaque datatype type_id with a unique ASCII identifier
 * tag.
 * 
 * @param type IN: Datatype identifier for the opaque datatype to be tagged.
 * @param tag  IN: Descriptive ASCII string with which the opaque datatype is to be tagged.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tset_tag(int type, String tag)
        throws HDF5LibraryException;

/**
 *  H5Tinsert adds another member to the compound datatype type_id.
 *
 *  @param type_id  IN: Identifier of compound datatype to modify.
 *  @param name     IN: Name of the field to insert.
 *  @param offset   IN: Offset in memory structure of the field to insert.
 *  @param field_id IN: Datatype identifier of the field to insert.
 * 
 *  @return a non-negative value if successful
 * 
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
public synchronized static native int H5Tinsert(int type_id, String name,
        long offset, int field_id)
        throws HDF5LibraryException, NullPointerException;

/**
 *  H5Tis_variable_str determines whether the datatype identified in type_id is a variable-length string. 
 *
 *  @param type_id  IN: Identifier of datatype to query.
 *
 *  @return true if type_id is a variable-length string. 
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native boolean H5Tis_variable_str(int type_id)
throws HDF5LibraryException;

/**
 * H5Tlock locks the datatype specified by the type_id identifier, making it
 * read-only and non-destrucible.
 * 
 * @param type_id IN: Identifier of datatype to lock.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tlock(int type_id)
        throws HDF5LibraryException;

/**
 *  H5Topen opens a named datatype at the location specified
 *  by loc_id and return an identifier for the datatype.
 *
 *  @deprecated As of HDF5 1.8, replaced by {@link #H5Topen(int, String, int)}
 *
 *  @param loc_id   IN: A file, group, or datatype identifier.
 *  @param name     IN: A datatype name, defined within the file or group identified by loc_id.
 *
 *  @return a named datatype identifier if successful
 *
 *  @exception HDF5LibraryException - Error from the HDF-5 Library.
 *  @exception NullPointerException - name is null.
 **/
@Deprecated
public static int H5Topen(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException
{
    int id = _H5Topen(loc_id, name);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Topen(int loc_id, String name)
        throws HDF5LibraryException, NullPointerException;

/**
*  H5Topen opens a named datatype at the location specified
*  by loc_id and return an identifier for the datatype.
*
*  @param loc_id   IN: A file, group, or datatype identifier.
*  @param name     IN: A datatype name, defined within the file or group identified by loc_id.
*  @param tapl_id  IN: Datatype access property list.
*
*  @return a named datatype identifier if successful
*
*  @exception HDF5LibraryException - Error from the HDF-5 Library.
*  @exception NullPointerException - name is null.
**/
public static int H5Topen(int loc_id, String name, int tapl_id)
throws HDF5LibraryException, NullPointerException
{
    int id = _H5Topen2(loc_id, name, tapl_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Topen2(int loc_id, String name, int tapl_id)
throws HDF5LibraryException, NullPointerException;

/**
 * H5Tpack recursively removes padding from within a compound datatype to
 * make it more efficient (space-wise) to store that data.
 * <P>
 * <b>WARNING:</b> This call only affects the C-data, even if it succeeds,
 * there may be no visible effect on Java objects.
 * 
 * @param type_id IN: Identifier of datatype to modify.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public synchronized static native int H5Tpack(int type_id)
        throws HDF5LibraryException;

//public synchronized static native int H5Tregister(H5T_pers_t pers, String name, int src_id, int dst_id,
//H5T_conv_t func)
//throws HDF5LibraryException, NullPointerException;
//
//public synchronized static native int H5Tunregister(H5T_pers_t pers, String name, int src_id, int dst_id,
//H5T_conv_t func)
//throws HDF5LibraryException, NullPointerException;

/**
 * H5Tvlen_create creates a new variable-length (VL) dataype.
 * 
 * @param base_id  IN: Identifier of parent datatype.
 * 
 * @return a non-negative value if successful
 * 
 * @exception HDF5LibraryException - Error from the HDF-5 Library.
 **/
public static int H5Tvlen_create(int base_id) throws HDF5LibraryException
{
    int id = _H5Tvlen_create(base_id);
    if (id > 0)
        OPEN_IDS.addElement(id);
    return id;
}

private synchronized static native int _H5Tvlen_create(int base_id)
        throws HDF5LibraryException;

//////////////////////////////////////////////////////////////
////
//H5Z: Filter Interface Functions //
////
//////////////////////////////////////////////////////////////

public synchronized static native int H5Zfilter_avail(int filter)
        throws HDF5LibraryException, NullPointerException;

public synchronized static native int H5Zget_filter_info(int filter)
        throws HDF5LibraryException;

public synchronized static native int H5Zunregister(int filter)
        throws HDF5LibraryException, NullPointerException;

}
