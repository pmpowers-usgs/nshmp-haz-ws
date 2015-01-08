package gov.usgs.earthquake.nshm.www.services;

import static org.opensha.gmm.Imt.*;
import static com.google.common.base.StandardSystemProperty.LINE_SEPARATOR;
import static org.opensha.programs.HazardCurve.calc;
import gov.usgs.earthquake.nshm.www.util.ModelID;
import gov.usgs.earthquake.param.Param;
import gov.usgs.earthquake.param.ParamList;
import gov.usgs.earthquake.param.Params;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensha.calc.HazardResult;
import org.opensha.calc.Site;
import org.opensha.eq.model.HazardModel;
import org.opensha.geo.GeoTools;
import org.opensha.geo.Location;
import org.opensha.gmm.Imt;
import org.opensha.util.Parsing;
import org.opensha.util.Parsing.Delimiter;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Servlet implementation class HazardCurve
 */
@WebServlet("/HazardCurve/*")
public class HazardCurve extends HttpServlet {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// TODO logging; servlet will only use system ConsoleHandler
	// and Formatter; need to set up our custom console handler as a
	// fileHandler independent of tomcat request logs; config
	// should be automagically read from classes/logging.properties
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		response.setContentType("text/html");
		
		String query = request.getPathInfo();
		if (query == null) {
			response.getWriter().print(USAGE);
			return;
		}
		
		List<String> args = Parsing.splitToList(query, Delimiter.SLASH);
		if (args.size() == 0) {
			response.getWriter().print(USAGE);
			return;
		}
		if (args.size() != 5) {
			response.getWriter().print(USAGE);
			return;
		}
		
		String result = processRequest(args);
		response.getWriter().print(result);
	}

	private String processRequest(List<String> args) {
		String modelStr = "NSHMP_" + args.get(1) + "_" + args.get(0);
		ModelID modelId = ModelID.valueOf(modelStr);
		HazardModel model = modelId.instance();
		if (model == null) return "Model " + modelId + " not currently supported";
		Imt imt = Imt.valueOf(args.get(2));
		double lon = Double.valueOf(args.get(3));
		double lat = Double.valueOf(args.get(4));
		Location loc = Location.create(lat, lon);
		Site site = Site.create(loc, 760.0);
		HazardResult result = calc(model, imt, site);
		String curve = Parsing.join(result.curve().xValues(), Delimiter.COMMA) + LINE_SEPARATOR.value() +
			Parsing.join(result.curve().yValues(), Delimiter.COMMA);
		return curve;
	}

	private static final String USAGE;

	static {
		StringBuilder sb = new StringBuilder("HazardCurve usage:<br/><br/>");
		sb.append("A USGS hazard curve may be computed by supplying a slash-delimited<br/>");
		sb.append("query consisting of year, model, intensity measure type (imt), longitude,<br/>");
		sb.append("and latitude. For <a href=\"/nshmp-haz-ws/HazardCurve/2008/WUS/PGA/-118.25/34.05\">example</a>:<br/><br/>");
		sb.append("&nbsp;&nbsp;<code>http://.../nshmp-haz-ws/HazardCurve/2008/WUS/PGA/-118.25/34.05</code><br/><br/>");
		sb.append("where:<br/>");
		sb.append("&nbsp;&nbsp;year = [2008, 2014]<br/>");
		sb.append("&nbsp;&nbsp;model = [WUS, CEUS]<br/>");
		sb.append("&nbsp;&nbsp;imt = see <a href=\"http://usgs.github.io/nshmp-haz/index.html?org/opensha/gmm/Imt.html\">docs</a> for options<br/>");
		USAGE = sb.toString();
	}
	
	// @formatter:off
	
	private static final String ANGLE_UNIT = "°";
	
	static class Parameters {

		ParamList pList;
		
		private Parameters() {
			
			Param<ModelID> modelParam = Params.newEnumParam(
				"Hazard Model",
				"USGS hazard model and year identifier",
				ModelID.WUS_2008,
				EnumSet.allOf(ModelID.class));
				
			Param<Imt> imtParam = Params.newEnumParam(
				"Intensity Measure",
				"USGS hazard model and year identifier",
				PGA,
				EnumSet.of(PGA, SA0P1, SA0P2, SA0P3, SA0P5, SA1P0, SA2P0, SA3P0));
				
			Param<Integer> vsParam = Params.newIntegerParamWithValues(
				"Vs30",
				"The Vs30 at the site of interest",
				"m/s",
				760,
				ImmutableSet.of(180, 259, 360, 537, 760, 1150, 2000));

			Param<Double> latParam = Params.newDoubleParamWithBounds(
				"Latitude",
				"Latitude of site, in degrees",
				ANGLE_UNIT,
				34.0,
				GeoTools.MIN_LAT,
				GeoTools.MAX_LAT);

			Param<Double> lonParam = Params.newDoubleParamWithBounds(
				"Longitude",
				"Longitude of site, in degrees",
				ANGLE_UNIT,
				-118.2,
				GeoTools.MIN_LON,
				GeoTools.MAX_LON);
						
			pList = ParamList.of(modelParam, imtParam, vsParam, latParam, lonParam);
			
		}
	}
	
	public static void main(String[] args) {
		Parameters p = new Parameters();
		System.out.println(GSON.toJson(p.pList.state()));
	}

}
