/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.rrm.ehour.timesheet.service;

import net.rrm.ehour.config.EhourConfig;
import net.rrm.ehour.data.DateRange;
import net.rrm.ehour.domain.*;
import net.rrm.ehour.persistence.timesheet.dao.TimesheetCommentDao;
import net.rrm.ehour.persistence.timesheet.dao.TimesheetDao;
import net.rrm.ehour.project.service.ProjectAssignmentService;
import net.rrm.ehour.report.reports.element.AssignmentAggregateReportElement;
import net.rrm.ehour.report.service.AggregateReportService;
import net.rrm.ehour.timesheet.dto.BookedDay;
import net.rrm.ehour.util.DateUtil;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import scala.collection.immutable.Vector;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings( { "deprecation" })
public class TimesheetServiceImplTest
{
	private TimesheetServiceImpl timesheetService;
	private TimesheetDao timesheetDAO;
	private TimesheetCommentDao timesheetCommentDAO;
	private EhourConfig config;
	private AggregateReportService aggregateReportService;
	private ProjectAssignmentService projectAssignmentService;
    private TimesheetLockService timesheetLockService;

	@Before
	public void setUp()
	{

		config = createMock(EhourConfig.class);
		timesheetDAO = createMock(TimesheetDao.class);
		aggregateReportService = createMock(AggregateReportService.class);
		timesheetCommentDAO = createMock(TimesheetCommentDao.class);
		projectAssignmentService = createMock(ProjectAssignmentService.class);
        timesheetLockService = createMock(TimesheetLockService.class);

        timesheetService = new TimesheetServiceImpl(timesheetDAO, timesheetCommentDAO, timesheetLockService,
                                                    aggregateReportService, projectAssignmentService, config);
	}

	@Test
	public void should_get_booked_days_for_february() throws Exception
	{
		Calendar cal = new GregorianCalendar(2006, 10, 5);

		BookedDay dayA = new BookedDay();
		dayA.setDate(new Date(2006 - 1900, 10, 1));
		dayA.setHours((float) 6);

        BookedDay  dayB = new BookedDay();
		dayB.setDate(new Date(2006 - 1900, 10, 2));
		dayB.setHours((float) 8);

		expect(timesheetDAO.getBookedHoursperDayInRange(1, DateUtil.calendarToMonthRange(cal))).andReturn(Arrays.asList(dayA, dayB));
		expect(config.getCompleteDayHours()).andReturn(8f).times(2);

		replay(config);

		replay(timesheetDAO);

        List<LocalDate> results = timesheetService.getBookedDaysMonthOverview(1, cal);

        verify(timesheetDAO);
		verify(config);

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getDayOfMonth());
    }

	@Test
	public void should_get_timesheet_overview() throws Exception
	{
		List<TimesheetEntry> daoResults = new ArrayList<TimesheetEntry>();
		List<AssignmentAggregateReportElement> reportResults = new ArrayList<AssignmentAggregateReportElement>();
		Calendar cal = new GregorianCalendar();

		TimesheetEntry entryA, entryB;
		TimesheetEntryId idA, idB;

		idA = new TimesheetEntryId(new Date(2006 - 1900, 10 - 1, 2), null);
		entryA = new TimesheetEntry();
		entryA.setEntryId(idA);
		entryA.setHours((float) 5);
		daoResults.add(entryA);

		idB = new TimesheetEntryId(new Date(2006 - 1900, 10 - 1, 6), null);
		entryB = new TimesheetEntry();
		entryB.setEntryId(idB);
		entryB.setHours((float) 3);
		daoResults.add(entryB);

		AssignmentAggregateReportElement agg = new AssignmentAggregateReportElement();
		ProjectAssignment pa = ProjectAssignmentObjectMother.createProjectAssignment(0);
		agg.setProjectAssignment(pa);
		reportResults.add(agg);

		expect(timesheetDAO.getTimesheetEntriesInRange(1, DateUtil.calendarToMonthRange(cal))).andReturn(daoResults);

		expect(aggregateReportService.getHoursPerAssignmentInRange(1, DateUtil.calendarToMonthRange(cal))).andReturn(reportResults);

		replay(timesheetDAO);
		replay(aggregateReportService);

		timesheetService.getTimesheetOverview(new User(1), cal);

		verify(timesheetDAO);
		verify(aggregateReportService);
	}

	@Test
	public void should_get_timesheet_entries()
	{
		Date da = new Date(2006 - 1900, 12 - 1, 31);
		Date db = new Date(2007 - 1900, 1 - 1, 6);
		DateRange range = new DateRange(da, db);

		DateRange rangeB = new DateRange(new Date(2006 - 1900, 12 - 1, 31), new Date(2007 - 1900, 1 - 1, 6));

		expect(timesheetDAO.getTimesheetEntriesInRange(1, range)).andReturn(new ArrayList<TimesheetEntry>());
		expect(timesheetCommentDAO.findById(new TimesheetCommentId(1, range.getDateStart()))).andReturn(new TimesheetComment());
		expect(projectAssignmentService.getProjectAssignmentsForUser(1, rangeB)).andReturn(new ArrayList<ProjectAssignment>());
        expect(config.getFirstDayOfWeek()).andReturn(1);
        expect(timesheetLockService.findLockedDatesInRange(anyObject(Date.class), anyObject(Date.class))).andReturn(new Vector<Interval>(0, 0, 1));

		replay(timesheetDAO, timesheetCommentDAO, projectAssignmentService, config, timesheetLockService);

		timesheetService.getWeekOverview(new User(1), new GregorianCalendar(2007, 1 - 1, 1));

		verify(timesheetDAO);
		verify(timesheetCommentDAO);
		verify(projectAssignmentService);
        verify(config);
        verify(timesheetLockService);
    }
}
