/*
 * Copyright 2022 Ruchi Dhore
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

import React, {useState} from 'react'
import './App.css';
import { Form, Container, Row, Col, ButtonGroup, Button } from 'react-bootstrap';
import bookLogo from './images/book.jpg';
import axios from 'axios';

function App() {
  const [file, setFile] = useState();
  let isDownload = true;
  const backendURL = "http://cc-project-lb-1364801742.us-east-1.elb.amazonaws.com";

  return (
    <div>
      <div className="player">
        <div className="avatar">
          <img src={bookLogo} width="800" height="350" alt="Stack of Books"/>
        </div>
        <Container>
          <Row>
            <Col className="c1">
              <Form.Group controlId="formFile" className="mb-3">
                <Form.Control type="file" onChange={handleChange} />
              </Form.Group>
            </Col>
            <Col>
              <input id="convertButton" type="button" className="customBtn1" onClick={handleSubmit} value="Convert" />
            </Col>
          </Row>
        </Container>
        <Container>
          <Row>
            <Col md="auto customCol">
              <ButtonGroup id="buttonGroup" aria-label="Basic example" className="customBtnGrp">
                <Button id="english" variant="info" className="customBtn2" onClick={playAudio}>English(US)</Button>
                <Button id="hindi" variant="info" className="customBtn2" onClick={playAudio}>Hindi</Button>
                <Button id="chinese" variant="info" className="customBtn2" onClick={playAudio}>Chinese</Button>
              </ButtonGroup>
            </Col>
            <Col md="auto">
              <Form>
                <Form.Check 
                  type="switch" id="customSwitch" 
                  label="Download Audio" onChange={handleDownload}/>
              </Form>
            </Col>
          </Row>
        </Container>
        <Container>
          <Row>
            <Col md="auto">
              <audio controls id="audio-player" src="" className="customAudio">
                Your browser does not support the <code>audio</code> element.
              </audio>
            </Col>
          </Row>
        </Container>
      </div>
    </div>
  )

  function handleChange(event) {
    setFile(event.target.files[0])
  }

  function handleSubmit(event) {
    event.preventDefault();
    const url = backendURL + '/file/upload';
    const formData = new FormData();
    formData.append('file', file);
    formData.append('fileName', file.name);
    axios.post(url, formData).then((response) => {
      console.log(response.data);
      if (response.status === 200) {
        console.log("File send to backend successfully");
      } else {
        console.log("Something went wrong");
      }
    });
  }

  function handleDownload(event) {
    console.log(event.target.checked);
    if (event.target.checked === true) {
      isDownload = true;
    } 
    
    if (event.target.checked === false) {
      isDownload = false;
    }
  }

  function playAudio(event) {
    let temp = file.name.split(".");
    let expectedFileName = temp[0] + "_" + event.currentTarget.id + ".mp3";
    const url = backendURL + "/file/find/";
    const params = { 
      filename: expectedFileName 
    };
    axios.get(url, {params}).then((response) => {
      let isPresent = JSON.stringify(response.data).includes("audio file present");
      if (isPresent) {
        console.log("Audio file generated in S3 bucket");
        let audioUrl = "https://cc-audio-bucket.s3.amazonaws.com//tmp/" + expectedFileName;
        let audioPlayer = document.getElementById('audio-player');
        audioPlayer.src = audioUrl;

        // Download audio file
        if (isDownload) {
            const downloadUrl = backendURL + "/file/download/";
            const params = { 
              filename: expectedFileName 
            };
            const config = { responseType: 'blob' };
            axios.get(downloadUrl, {params}, config).then((response) => {
                  const url = window.URL.createObjectURL(new Blob([response]),);
                  const link = document.createElement('a');
                  link.href = url;
                  link.setAttribute('download', expectedFileName,);
                  document.body.appendChild(link);
                  link.click();
                  link.parentNode.removeChild(link);
            })
        }
      
        audioPlayer.play();
      } else {
        console.log("Audio file not ready");
      }
    })
  }
}

export default App;
