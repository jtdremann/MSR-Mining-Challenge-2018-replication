# Install needed packages ahead of time
if (!require("pacman")) install.packages("pacman")
pacman::p_load(plyr, ggplot2, reshape2)

f <- file.choose() #continuous

logs <- read.csv(f)

names(logs)

logs$orderedDaysOfWeek<-factor(logs$dayOfWeek,levels=levels(logs$dayOfWeek)[c(2, 6, 7, 5, 1, 3, 4)])

#logs$dayOfWeek<-factor(logs$dayOfWeek, c("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"))

plot(logs$orderedDaysOfWeek)

#sum(logs$totalBuildTargets)

logs$successRate <- ( (logs$buildTargetSuccesses + logs$testCaseSuccesses) / (logs$totalBuildTargets + logs$totalTestCaseResults) * 100 )

logs$failureRate <- ( (logs$buildTargetFails + logs$testCaseErrors + logs$testCaseFails) / (logs$totalBuildTargets + logs$totalTestCaseResults) * 100 )

#Create new df for non NaN success rates
sRate = logs[is.nan(logs$successRate) == FALSE,]

agg_success <- aggregate(x=list(Success_Rate=sRate$successRate),
                    by=list(Day=sRate$orderedDaysOfWeek),
                    FUN=mean)

agg_failure <- aggregate(x=list(Failure_Rate=sRate$failureRate),
                    by=list(Day=sRate$orderedDaysOfWeek),
                    FUN=mean)

agg_success$Failure_Rate = agg_failure$Failure_Rate

# Basic line plot with points of success rate
ggplot(data=agg_success, aes(x=Day, y=Success_Rate, group=1)) +
  geom_line() + aes(y = Success_Rate) + geom_line(aes(y = Failure_Rate)) +
  ylim(0, 100)


#Look at how many have test case errors?


#File 2


f2 <- file.choose()

logs2 <- read.csv(f2)

names(logs2)


