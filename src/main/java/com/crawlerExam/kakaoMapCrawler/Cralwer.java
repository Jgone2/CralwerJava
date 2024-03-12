package com.crawlerExam.kakaoMapCrawler;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class Cralwer {

    public static void main(String[] args) throws InterruptedException {

        // 크롤링할 사이트 주소
        final String CRAWLER_URL = "https://map.kakao.com/";

        // 웹 로딩 최대 대기 시간
        final Duration TIME_OUT = Duration.ofSeconds(5);


        String filePath = verifyExistingFile();

        // 크롬 드라이버 위치 설정
        System.setProperty("webdriver.chrome.driver", filePath);

        // 크롬 옵션 설정
        ChromeOptions options = setChromeOptions();
        log.info("크롬 옵션 설정 완료!");

        // WebDriver 객체 생성
        WebDriver driver = null;
        WebDriverWait wait = null;
        try {
            driver = new ChromeDriver(options);

            // 로드 최대 대기 시간 설정
            wait = new WebDriverWait(driver, TIME_OUT);

            // 드라이버를 사용한 추가 작업
            // 브라우저 열기
            driver.get(CRAWLER_URL);
            log.info("드라이버 열었숨!");

            // 카카오맵 검색창 찾기(검색창 id: search.keyword.query) 및 로드될 때까지 대기
            WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("search.keyword.query")));

            // 검색어 입력
            searchBox.sendKeys("강남역 음식점" + Keys.ENTER);
            log.info("검색어 입력했하고 엔터까지 완료!");

            crawlAllPages(driver, wait);

        } catch (Exception e) {
            log.info("드라이버 생성 실패", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static void crawlAllPages(WebDriver driver, WebDriverWait wait) {
        while (true) {
            try {
                // 웹 요소 로드 대기
                List<WebElement> listItems = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//ul[@id='info.search.place.list']//li")));

                // 웹 요소의 수
                int size = listItems.size();

                // 각 웹 요소 처리
                for (int i = 0; i < size; i++) {
                    // 웹 요소 목록 다시 로드
                    listItems = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//ul[@id='info.search.place.list']//li")));

                    // 웹 요소에 대한 작업 수행
                    WebElement listItem = listItems.get(i).findElement(By.xpath(".//div[@class='info_item']//div[@class='contact clickArea']//a[@class='moreview']"));
                    JavascriptExecutor executor = (JavascriptExecutor)driver;
                    executor.executeScript("arguments[0].click();", listItem);

                    // 새 탭으로 전환
                    String base = driver.getWindowHandle();
                    Set<String> set = driver.getWindowHandles();
                    set.remove(base);
                    assert set.size() == 1;
                    String newTab = (String) set.toArray()[0];
                    driver.switchTo().window(newTab);

                    // 새 탭에서 크롤링 작업 수행
                    getAttr(wait);

                    // 새 탭 닫기
                    driver.close();

                    // 원래 탭으로 전환
                    driver.switchTo().window(base);
                }

                // 다음 페이지로 이동
                clickNextPage(driver, wait);

                // 페이지 로딩 대기
                Thread.sleep(2000); // 페이지 로딩 시간에 따라 조정이 필요합니다.

            } catch (TimeoutException e) {
                // "다음" 버튼이 더 이상 없는 경우 크롤링 종료
                log.error("모든 페이지를 크롤링했습니다.", e);
                break;
            } catch (InterruptedException e) {
                log.error("페이지 로딩 대기 중 오류가 발생했습니다.", e);
                break;
            }
        }
    }

    private static ChromeOptions setChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");   // headless 모드 설정(브라우저를 띄우지 않음)
        options.addArguments("disable-gpu");    // GPU 사용 안함
        options.addArguments("no-sandbox"); // sandbox 모드 disabled
        options.addArguments("--disable-popup-blocking");    // 모든 팝업 허용
        options.addArguments("disable-dev-shm-usage");   // overcome limited resource problems
        options.addArguments("--remote-allow-origins-*");
        return options;
    }

    private static void getAttr(WebDriverWait wait) {
        WebElement locationDetail = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='mArticle']//div[@class='location_detail']//span[@class='txt_address']")));
        WebElement locationNum = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='mArticle']//div[@class='location_detail']//span[@class='txt_addrnum']")));

        System.out.println(locationDetail.getText());
        System.out.println(locationNum.getText());
    }

    // 다음 페이지로 이동
    private static void clickNextPage(WebDriver driver, WebDriverWait wait) {
        try {
            // "더보기" 버튼이나 페이지 버튼 찾기
            WebElement moreButton = driver.findElement(By.id("info.search.place.more"));
            List<WebElement> nextButtons = driver.findElements(By.id("info.search.page.next"));
            List<WebElement> pageButtons = driver.findElements(By.cssSelector("a[id^='info.search.page.no']"));

            // 현재 페이지 번호 및 마지막 페이지 번호 계산
            int currentPageNumber = getCurrentPageNumber(driver, pageButtons);
            int lastPageNumber = pageButtons.size();

            // 5페이지인 경우 다음 페이지 버튼 클릭
            if (currentPageNumber == lastPageNumber) {
                System.out.println("========= 다음 버튼 클릭 ===============");
                WebElement nextButton = nextButtons.get(0);
                JavascriptExecutor executor = (JavascriptExecutor) driver;
                executor.executeScript("arguments[0].click();", nextButton);
            } else if (currentPageNumber < lastPageNumber) {
                // "더보기" 버튼 또는 페이지 버튼 클릭
                JavascriptExecutor executor = (JavascriptExecutor) driver;
                if (moreButton.isDisplayed()) {
                    System.out.println("========= 더보기 버튼 클릭 ===============");
                    executor.executeScript("arguments[0].click();", moreButton);
                } else {
                    System.out.println("========= 다음 페이지 버튼 클릭 ===============");
                    WebElement nextButton = pageButtons.get(currentPageNumber);
                    executor.executeScript("arguments[0].click();", nextButton);
                }
            }

            // 페이지 로딩 완료 확인
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//ul[@id='info.search.place.list']//li")));
        }  catch (Exception e) {
            log.error("페이지 이동 중 오류가 발생했습니다.", e);
        }
    }

    private static int getCurrentPageNumber(WebDriver driver, List<WebElement> pageButtons) {
        int currentPageNumber = 0;
        for (int i = 0; i < pageButtons.size(); i++) {
            if (pageButtons.get(i).getAttribute("class").equals("ACTIVE")) {
                currentPageNumber = i + 1;
                break;
            }
        }
        return currentPageNumber;
    }

    private static String verifyExistingFile() {
        String filePath = "/Users/jgone2/spring/kakaoMapCrawler/chromedriver";
        File file = new File(filePath);

        if (file.exists()) {
            System.out.println("파일이 존재합니다: " + filePath);
        } else {
            System.out.println("파일이 존재하지 않습니다: " + filePath);
        }
        return filePath;
    }
}